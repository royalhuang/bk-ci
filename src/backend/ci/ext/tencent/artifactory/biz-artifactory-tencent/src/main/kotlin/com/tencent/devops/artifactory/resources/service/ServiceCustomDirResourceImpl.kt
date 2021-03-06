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

import com.tencent.devops.artifactory.api.service.ServiceCustomDirResource
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.service.artifactory.ArtifactoryCustomDirGsService
import com.tencent.devops.artifactory.service.bkrepo.BkRepoCustomDirGsService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.RepoGray
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceCustomDirResourceImpl @Autowired constructor(
    private val artifactoryCustomDirGsService: ArtifactoryCustomDirGsService,
    private val bkRepoCustomDirGsService: BkRepoCustomDirGsService,
    private val redisOperation: RedisOperation,
    private val repoGray: RepoGray
) : ServiceCustomDirResource {

    override fun getGsDownloadUrl(projectId: String, fileName: String, userId: String): Result<Url> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (fileName.isBlank()) {
            throw ParamBlankException("Invalid fileName")
        }
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }

        return if (repoGray.isGray(projectId, redisOperation)) {
            Result(Url(bkRepoCustomDirGsService.getDownloadUrl(projectId, fileName, userId)))
        } else {
            Result(Url(artifactoryCustomDirGsService.getDownloadUrl(projectId, fileName, userId)))
        }
    }
}