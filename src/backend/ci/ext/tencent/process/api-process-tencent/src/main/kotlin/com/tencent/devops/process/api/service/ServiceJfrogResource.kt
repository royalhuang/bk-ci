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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_JFROG"], description = "服务-jfrog资源")
@Path("/service/jfrog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceJfrogResource {

    @ApiOperation("根据流水线id获取流水线名字")
    @POST
    // @Path("/projects/{projectId}/getPipelineNames")
    @Path("/{projectId}/getPipelineNames")
    fun getPipelineNameByIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id列表", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("根据构建id获取构建号")
    @POST
    // @Path("/projects/{projectId}/pipelines/{pipelineId}/getBuildNos")
    @Path("/{projectId}/{pipelineId}/getBuildNos")
    fun getBuildNoByBuildIds(
        @ApiParam("项目id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("构建id列表", required = true)
        buildIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("根据流水线id和构建id对，获取build num")
    @POST
    // @Path("/projects/{projectId}/getBuildNoByBuildIds")
    @Path("/{projectId}/getBuildNoByBuildIds")
    fun getBuildNoByBuildIds(
        @ApiParam("项目id", required = true)
        buildIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("获取时间段内有构建产物的构建数量")
    @GET
    // @Path("/countArtifactoryByTime")
    @Path("/getArtifactoryByTime/count")
    fun getArtifactoryCountFromHistory(
        @ApiParam("起始时间", required = true)
        @QueryParam("startTime")
        startTime: Long,
        @ApiParam("终止时间", required = true)
        @QueryParam("endTime")
        endTime: Long
    ): Result<Int>
}