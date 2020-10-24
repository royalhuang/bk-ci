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
package com.tencent.devops.openapi.service.op

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.pojo.AppCodeGroup
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * @Description
 * @Date 2020/3/17
 * @Version 1.0
 */
@Service
class AppCodeService(
        private val client: Client,
        private val appCodeGroupService: AppCodeGroupService,
        private val appCodeProjectService: AppCodeProjectService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AppCodeService::class.java)
    }

    private val appCodeProjectCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build<String/*appCode*/, Map<String, String>/*Map<projectId,projectId>*/>(
                    object : CacheLoader<String, Map<String, String>>() {
                        override fun load(appCode: String): Map<String, String> {
                            return try {
                                val projectMap = getAppCodeProject(appCode)
                                logger.info("appCode[$appCode] openapi projectMap:$projectMap.")
                                projectMap
                            } catch (t: Throwable) {
                                logger.info("appCode[$appCode] failed to get projectMap.")
                                mutableMapOf()
                            }
                        }
                    }
            )

    private val appCodeGroupCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build<String/*appCode*/, Pair<String, List<AppCodeGroup>?>/*Map<projectId,AppCodeGroup>*/>(
                    object : CacheLoader<String, Pair<String, List<AppCodeGroup>?>>() {
                        override fun load(appCode: String): Pair<String, List<AppCodeGroup>?> {
                            try {
                                val appCodeGroupList = getAppCodeGroup(appCode)
                                logger.info("appCode[$appCode] openapi appCodeGroupList:$appCodeGroupList.")
                                return Pair(appCode, appCodeGroupList)
                            } catch (t: Throwable) {
                                logger.info("appCode[$appCode] failed to get appCodeGroup.")
                                return Pair(appCode, null)
                            }
                        }
                    }
            )

    private val projectInfoCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build<String/*appCode*/, Pair<String, ProjectVO?>/*Map<projectId,projectId>*/>(
                    object : CacheLoader<String, Pair<String, ProjectVO?>>() {
                        override fun load(projectId: String): Pair<String, ProjectVO?> {
                            return try {
                                val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
                                logger.info("projectId[$projectId] openapi projectInfo:$projectInfo.")
                                return Pair(projectId, projectInfo)
                            } catch (t: Throwable) {
                                logger.info("projectId[projectIdappCode] failed to get projectInfo.")
                                return Pair(projectId, null)
                            }
                        }
                    }
            )

    private fun getAppCodeProject(appCode: String): Map<String, String> {
        val projectList = appCodeProjectService.listProjectByAppCode(appCode)
        val result = mutableMapOf<String, String>()
        projectList.forEach {
            result[it.projectId] = it.projectId
        }
        return result
    }

    private fun getAppCodeGroup(appCode: String): List<AppCodeGroup>? {
        val appCodeGroupResponseList = appCodeGroupService.getGroups(appCode)
        return if (appCodeGroupResponseList == null || appCodeGroupResponseList.isEmpty()) {
            null
        } else {
            val appCodeGroupList = mutableListOf<AppCodeGroup>()
            appCodeGroupResponseList.forEach {
                appCodeGroupList.add(AppCodeGroup(
                        appCode = it.appCode,
                        bgId = it.bgId,
                        bgName = it.bgName,
                        deptId = it.deptId,
                        deptName = it.deptName,
                        centerId = it.centerId,
                        centerName = it.centerName
                ))
            }
            appCodeGroupList
        }
    }

    fun validAppCode(appCode: String, projectId: String): Boolean {
        val appCodeProject = appCodeProjectCache.get(appCode)
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache:$appCodeProject.")
        if (appCodeProject.isNotEmpty()) {
            val projectId = appCodeProject[projectId]
            if (projectId != null && projectId.isNotBlank()) {
                logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache matched.")
                return true
            }
        }
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeProjectCache no matched.")
        val appCodeGroupList = appCodeGroupCache.get(appCode).second
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache:$appCodeGroupList.")
        if (appCodeGroupList != null && appCodeGroupList.isNotEmpty()) {
            val projectInfo = projectInfoCache.get(projectId).second
            logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache projectInfo:$projectInfo.")

            if (projectInfo != null) {
                appCodeGroupList.forEach { appCodeGroup ->
                    if (appCodeGroup.centerId != null && projectInfo.centerId != null && appCodeGroup.centerId.toString() == projectInfo.centerId) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache centerId matched.")
                        return true
                    }
                    if (appCodeGroup.deptId != null && projectInfo.deptId != null && appCodeGroup.deptId.toString() == projectInfo.deptId) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache deptId matched.")
                        return true
                    }
                    if (appCodeGroup.bgId != null && projectInfo.bgId != null && appCodeGroup.bgId.toString() == projectInfo.bgId) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache bgId matched.")
                        return true
                    }
                    if (appCodeGroup.centerId == null && appCodeGroup.deptId == null && appCodeGroup.bgId == null) {
                        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache all group matched.")
                        return true
                    }
                }

            }
        }
        logger.info("appCode[$appCode] projectId[$projectId] openapi appCodeGroupCache no matched.")
        return false
    }
}