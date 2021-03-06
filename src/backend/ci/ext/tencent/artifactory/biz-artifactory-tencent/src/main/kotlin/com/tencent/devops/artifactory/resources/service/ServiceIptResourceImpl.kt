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

package com.tencent.devops.artifactory.resources.service

import com.tencent.devops.artifactory.api.service.ServiceIptResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.SearchProps
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryDownloadService
import com.tencent.devops.artifactory.service.artifactory.ArtifactorySearchService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoDownloadService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoSearchService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceIptResourceImpl @Autowired constructor(
    private val bkRepoSearchService: BkRepoSearchService,
    private val artifactorySearchService: ArtifactorySearchService,
    private val artifactoryDownloadService: ArtifactoryDownloadService,
    private val bkRepoDownloadService: BkRepoDownloadService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : ServiceIptResource {
    override fun searchFileAndProperty(
        userId: String,
        projectId: String,
        searchProps: SearchProps
    ): Result<FileInfoPage<FileInfo>> {
        val pipelineId = searchProps.props["pipelineId"]!!
        val buildId = searchProps.props["buildId"]!!
        val result = if (repoGray.isGray(projectId, redisOperation)) {
            bkRepoSearchService.searchFileAndProperty(userId, projectId, searchProps)
        } else {
            artifactorySearchService.searchFileAndProperty(userId, projectId, searchProps)
        }

        // 获取第三方下载链接
        result.second.forEach {
            val path = if (it.artifactoryType == ArtifactoryType.PIPELINE) {
                it.name
            } else {
                it.path
            }
            it.downloadUrl = if (repoGray.isGray(projectId, redisOperation)) {
                bkRepoDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    it.artifactoryType,
                    path,
                    null,
                    null,
                    null,
                    null,
                    null
                ).firstOrNull()
            } else {
                artifactoryDownloadService.getThirdPartyDownloadUrl(
                    projectId,
                    pipelineId,
                    buildId,
                    it.artifactoryType,
                    path,
                    null,
                    null,
                    null,
                    null,
                    null
                ).firstOrNull()
            }
        }

        return Result(FileInfoPage(result.second.size.toLong(), 0, 0, result.second, result.first))
    }
}