/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.support.api.op

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.app.NoticeRequest
import com.tencent.devops.support.model.app.pojo.Notice
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

@Api(tags = ["OP_NOTICE"], description = "OP-公告")
@Path("/op/notice")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpNoticeResource {

    @ApiOperation("获取所有公告信息")
    @GET
    @Path("/")
    fun getAllNotice(): Result<List<Notice>>

    @ApiOperation("获取公告信息")
    @GET
    @Path("/{id}")
    fun getNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Notice?>

    @ApiOperation("新增公告信息")
    @POST
    @Path("/")
    fun addNotice(
        @ApiParam(value = "公告请求报文体", required = true)
        noticeRequest: NoticeRequest
    ): Result<Int>

    @ApiOperation("更新公告信息")
    @PUT
    @Path("/{id}")
    fun updateNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long,
        @ApiParam(value = "公告请求报文体", required = true)
        noticeRequest: NoticeRequest
    ): Result<Int>

    @ApiOperation("删除公告信息")
    @DELETE
    @Path("/{id}")
    fun deleteNotice(
        @ApiParam(value = "公告id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Int>
}