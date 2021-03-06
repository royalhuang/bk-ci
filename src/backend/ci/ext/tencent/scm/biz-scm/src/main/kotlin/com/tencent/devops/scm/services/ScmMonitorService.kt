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

package com.tencent.devops.scm.services

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.enums.GatewayType
import com.tencent.devops.monitoring.api.service.StatusReportResource
import com.tencent.devops.monitoring.pojo.AddCommitCheckStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ScmMonitorService @Autowired constructor(
    private val client: Client
) {
    companion object {
        private val executorService = Executors.newFixedThreadPool(5)
        private val logger = LoggerFactory.getLogger(ScmMonitorService::class.java)
    }

    fun reportCommitCheck(
        requestTime: Long,
        responseTime: Long,
        statusCode: String?,
        statusMessage: String?,
        projectName: String,
        commitId: String
    ) {
        try {
            executorService.execute {
                sendReportCommitCheck(
                    requestTime = requestTime,
                    responseTime = responseTime,
                    statusCode = statusCode,
                    statusMessage = statusMessage,
                    projectName = projectName,
                    commitId = commitId
                )
            }
        } catch (e: Throwable) {
            logger.error(
                "report git commit check error, requestTime:$requestTime, " +
                    "responseTime:$responseTime, statusCode:$statusCode, statusMessage:$statusMessage", e
            )
        }
    }

    private fun sendReportCommitCheck(
        requestTime: Long,
        responseTime: Long,
        statusCode: String?,
        statusMessage: String?,
        projectName: String,
        commitId: String
    ) {
        try {
            client.getGateway(StatusReportResource::class, GatewayType.DEVNET_PROXY)
                .scmCommitCheck(
                    AddCommitCheckStatus(
                        requestTime = requestTime,
                        responseTime = responseTime,
                        elapseTime = responseTime - requestTime,
                        statusCode = statusCode,
                        statusMessage = statusMessage,
                        errorCode = CommonMessageCode.SUCCESS,
                        errorMsg = statusMessage,
                        projectName = projectName,
                        commitId = commitId
                    )
                )
        } catch (e: Throwable) {
            logger.error(
                "report git commit check error, requestTime:$requestTime, " +
                    "responseTime:$responseTime, statusCode:$statusCode, statusMessage:$statusMessage", e
            )
        }
    }
}