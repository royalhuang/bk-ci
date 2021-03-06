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
package com.tencent.devops.openapi.resources.apigw.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.pojo.external.measure.PipelineBuildResponseData
import com.tencent.devops.openapi.api.apigw.v2.ApigwBuildResourceV2
import com.tencent.devops.openapi.service.apigw.ApigwBuildService
import com.tencent.devops.openapi.service.apigw.ApigwBuildServiceV2
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwBuildResourceV2Impl @Autowired constructor(
    private val client: Client,
    private val apigwBuildService: ApigwBuildService,
    private val apigwBuildServiceV2: ApigwBuildServiceV2
) : ApigwBuildResourceV2 {
    override fun getBuildListByBG(
        appCode: String?,
        apigwType: String?,
        userId: String,
        bgId: String,
        beginDate: Long?,
        endDate: Long?,
        offset: Int?,
        limit: Int?
    ): Result<List<PipelineBuildResponseData>?> {
        return Result(
            apigwBuildServiceV2.getBuildList(
                userId = userId,
                bgId = bgId,
                beginDate = beginDate,
                endDate = endDate,
                offset = offset,
                limit = limit,
                interfaceName = "/{apigwType:apigw-user|apigw-app|apigw}/v2/builds/detail/list"
            )
        )
    }

    override fun stop(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<Boolean> {
        logger.info("Stop the build($buildId) of pipeline($pipelineId) of project($projectId) by user($userId)")
        return client.get(ServiceBuildResource::class).manualShutdown(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    override fun detail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<ModelDetail> {
        logger.info("get build detail: the build($buildId) of pipeline($pipelineId) of project($projectId) by user($userId)")
        return client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = ChannelCode.BS
        )
    }

    override fun getStatusWithoutPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        organizationType: String,
        organizationId: Long,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<BuildHistoryWithVars> {
        logger.info("Get the build($buildId) status of project($projectId) and pipeline($pipelineId) by user($userId)")
        return apigwBuildService.getStatusWithoutPermission(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            interfaceName = "/v2/builds/{projectId}/{pipelineId}/{buildId}/nopermission/status"
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwBuildResourceV2Impl::class.java)
    }
}