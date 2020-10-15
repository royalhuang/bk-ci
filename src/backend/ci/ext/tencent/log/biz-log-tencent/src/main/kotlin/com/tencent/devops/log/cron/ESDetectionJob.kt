/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.cron

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.es.ESClient
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.client.impl.MultiESLogClient
import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import com.tencent.devops.log.util.ESIndexUtils.getDocumentObject
import com.tencent.devops.log.util.ESIndexUtils.getIndexSettings
import com.tencent.devops.log.util.ESIndexUtils.getTypeMappings
import com.tencent.devops.log.util.IndexNameUtils
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.common.unit.TimeValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class ESDetectionJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val logClient: MultiESLogClient
) {

    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(2, TimeUnit.DAYS)
        .build<String/*index*/, Boolean>()
    private val executor = Executors.newCachedThreadPool()

    @Scheduled(initialDelay = 30000, fixedDelay = 30000)
    fun detect() {
        logger.info("Start to detect es")
        val redisLock = RedisLock(redisOperation, MULTI_LOG_CLIENT_DETECTION_LOCK_KEY, 30)
        try {
            if (redisLock.tryLock()) {
                val buildId = UUIDUtil.generate()
                val index = IndexNameUtils.getIndexName()
                logClient.getActiveClients().forEach {
                    try {
                        val f = executor.submit(Detection(it, index, buildId))
                        val documentIds = f.get(60, TimeUnit.SECONDS)
                        if (documentIds.isEmpty()) {
                            logger.warn("[${it.name}] Fail to insert data")
                            logClient.markESInactive(it.name)
                        } else {
                            executor.submit(Deletion(it, index, buildId, documentIds))
                        }
                    } catch (t: Throwable) {
                        logger.warn("[${it.name}] Fail to detect es status", t)
                        logClient.markESInactive(it.name)
                    }
                }
                val inactiveClients = logClient.getInactiveClients()
                logger.info("Start to check the inactive clients: ${inactiveClients.map { it.name }}")
                logClient.getInactiveClients().forEach {
                    try {
                        val f = executor.submit(Detection(it, index, buildId))
                        val documentIds = f.get(60, TimeUnit.SECONDS)
                        if (documentIds.isNotEmpty()) {
                            logger.warn("[${it.name}] Success to insert data")
                            logClient.markESActive(it.name)
                            executor.submit(Deletion(it, index, buildId, documentIds))
                        }
                    } catch (t: Throwable) {
                        logger.warn("[${it.name}] Fail to detect the inactive es", t)
                    }
                }
            } else {
                logger.info("Other instance took the key, ignore")
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun createIndex(esClient: ESClient, index: String) {
        val startEpoch = System.currentTimeMillis()
        logger.info("[${esClient.name}|$index] Create the index")
        try {
            val request = CreateIndexRequest(index)
                .settings(getIndexSettings())
                .mapping(getTypeMappings())
            request.setTimeout(TimeValue.timeValueSeconds(20))
            val response = esClient.client
                .indices()
                .create(request, RequestOptions.DEFAULT)
            logger.info("Get the create index response: $response")
        } catch (e: ElasticsearchStatusException) {
            logger.warn("Index already exist, ignore", e)
        } finally {
            logger.info("[${esClient.name}|$index] It took ${System.currentTimeMillis() - startEpoch}ms to create index")
        }
    }

    private fun addLines(esClient: ESClient, index: String, buildId: String): List<String> {
        val startEpoch = System.currentTimeMillis()
        try {
            logger.info("[${esClient.name}|$index|$buildId] Start to add lines")
            val bulkRequest = BulkRequest()
            for (i in 1 until MULTI_LOG_LINES) {
                val log = LogMessageWithLineNo(
                    tag = "test-tag-$i",
                    jobId = "job-$i",
                    message = "message lines - $i",
                    timestamp = System.currentTimeMillis(),
                    lineNo = i.toLong()
                )
                val indexRequest = genIndexRequest(
                    buildId = buildId,
                    logMessage = log,
                    index = index
                )
                if (indexRequest != null) {
                    indexRequest.create(false)
                        .timeout(TimeValue.timeValueSeconds(60))
                    bulkRequest.add(indexRequest)
                }
            }
            try {
                val bulkResponse = esClient.client.bulk(bulkRequest, RequestOptions.DEFAULT)
                if (bulkResponse.hasFailures()) {
                    logger.warn("[${esClient.name}|$index|$buildId] Fail to add lines: ${bulkResponse.buildFailureMessage()}")
                } else {
                    logger.info("[${esClient.name}|$index|$buildId] Success to add lines")
                }
                return bulkResponse.filter { !it.isFailed }.map { it.id }
            } catch (e: Exception) {
                logger.warn("[${esClient.name}|$index|$buildId] Fail to add lines", e)
            }
            return emptyList()
        } finally {
            logger.info("[${esClient.name}|$index|$buildId] It took ${System.currentTimeMillis() - startEpoch}ms to add lines")
        }
    }

    private fun genIndexRequest(
        buildId: String,
        logMessage: LogMessageWithLineNo,
        index: String
    ): IndexRequest? {
        val builder = getDocumentObject(buildId, logMessage)
        return try {
            IndexRequest(index).source(builder)
        } catch (e: IOException) {
            logger.error("[$buildId] Convert logMessage to es document failure", e)
            null
        } finally {
            builder.close()
        }
    }

    private inner class Detection(
        private val esClient: ESClient,
        private val index: String,
        private val buildId: String
    ) : Callable<List<String>> {

        override fun call(): List<String> {
            try {
                logger.info("[${esClient.name}|$index|$buildId] Start to the detection")
                // 1. Check if need to create index
                if (indexCache.getIfPresent(index) != true) {
                    createIndex(esClient, index)
                    indexCache.put(index, true)
                }
                // 2. insert data
                return addLines(esClient, index, buildId)
            } catch (t: Throwable) {
                logger.warn("[${esClient.name}|$index] Fail to do the es detection", t)
            }
            return emptyList()
        }
    }

    private inner class Deletion(
        private val esClient: ESClient,
        private val index: String,
        private val buildId: String,
        private val documentIds: List<String>
    ) : Runnable {
        override fun run() {
            val startEpoch = System.currentTimeMillis()
            try {
                logger.info("[${esClient.name}|$index|$buildId|$documentIds] Start to delete the record")
                if (documentIds.isEmpty()) {
                    logger.info("Empty document ids")
                    return
                }
                val bulkRequest = BulkRequest().timeout(TimeValue.timeValueSeconds(30))
                documentIds.forEach {
                    bulkRequest.add(DeleteRequest(index, it))
                }

                val bulkResponse = esClient.client.bulk(bulkRequest, RequestOptions.DEFAULT)
                if (bulkResponse.hasFailures()) {
                    logger.warn("[${esClient.name}|$index|$buildId] Fail to delete lines: $documentIds, ${bulkResponse.buildFailureMessage()}")
                } else {
                    logger.info("[${esClient.name}|$index|$buildId] Success to delete the records")
                }
            } finally {
                logger.info("[${esClient.name}|$index|$buildId] It took ${System.currentTimeMillis() - startEpoch}ms to delete the records ${documentIds.size}")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ESDetectionJob::class.java)
        private const val MULTI_LOG_CLIENT_DETECTION_LOCK_KEY = "log:multi:log:client:detection:lock:key"
        private const val MULTI_LOG_LINES = 10
    }
}