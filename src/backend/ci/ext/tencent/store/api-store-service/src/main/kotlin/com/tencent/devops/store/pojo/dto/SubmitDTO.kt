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

package com.tencent.devops.store.pojo.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubmitDTO(
    @ApiModelProperty("扩展服务id", required = true)
    val serviceId: String,
    @ApiModelProperty("扩展服务Code")
    val serviceCode: String,
    @ApiModelProperty("扩展服务Name")
    val serviceName: String,
    @ApiModelProperty("所属分类")
    val classify: String? = null,
    @ApiModelProperty("插件标签列表")
    val labelIdList: List<String>?,
    @ApiModelProperty("服务版本")
    val version: String,
    @ApiModelProperty("LOGO url")
    val logoUrl: String?,
    @ApiModelProperty("ICON")
    val icon: String?,
    @ApiModelProperty("扩展服务简介")
    val summary: String? = null,
    @ApiModelProperty("扩展服务描述")
    val description: String? = null,
    @ApiModelProperty("扩展服务发布者")
    val publisher: String?,
    @ApiModelProperty("是否是最后版本")
    val latestFlag: Int? = 0,
    @ApiModelProperty("描述录入类型")
    val descInputType: DescInputTypeEnum? = DescInputTypeEnum.MANUAL,
    @ApiModelProperty("发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正", required = true)
    val releaseType: ReleaseTypeEnum? = ReleaseTypeEnum.NEW,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String? = "",
    @ApiModelProperty("扩展点列表")
    val extensionItemList: Set<String>
)