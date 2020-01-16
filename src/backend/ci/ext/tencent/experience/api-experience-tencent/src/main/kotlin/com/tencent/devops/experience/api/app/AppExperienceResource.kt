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

package com.tencent.devops.experience.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_EXPERIENCE"], description = "版本体验-发布体验")
@Path("/app/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperienceResource {

    @ApiOperation("获取体验列表")
    @Path("/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页目", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数目", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<AppExperience>>

    @ApiOperation("获取体验详情")
    @Path("/{experienceHashId}/detail")
    @GET
    fun detail(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<AppExperienceDetail>

    @ApiOperation("创建外部直接下载链接")
    @Path("/{experienceHashId}/downloadUrl")
    @POST
    fun downloadUrl(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("体验ID", required = true)
        @PathParam("experienceHashId")
        experienceHashId: String
    ): Result<DownloadUrl>

    @ApiOperation("获取项目体验列表")
    @Path("/{projectId}/history")
    @GET
    fun history(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<AppExperienceSummary>>

    @ApiOperation("获取项目用户组信息和组所有人员")
    @Path("/{projectId}/projectGroupAndUsers")
    @GET
    fun projectGroupAndUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<ProjectGroupAndUsers>>

    @ApiOperation("创建体验")
    @Path("{projectId}")
    @POST
    fun creat(
        @ApiParam("用户Id", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目Id", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("体验详情", required = true)
        experience: ExperienceCreate
    ): Result<Boolean>
}