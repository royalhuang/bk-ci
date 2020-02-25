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

package com.tencent.devops.common.pipeline.type.devcloud

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType

/**
 * class PCGDockerImage(
 *   val img_ver: String,
 *   val os: String,
 *   val img_name: String,
 *   val language: String
 * )
 * image:
 * img_name:img_ver:os:language
 *
 * ie.
 * {
 *   "img_ver":"3.2.2.3.rc",
 *   "os":"tlinux",
 *   "img_name":"tc/tlinux/qbgdev",
 *   "language":"C++",
 * }
 * image:
 * tc/tlinux/qbgdev:3.2.2.3.rc:tlinux:C++
 */
data class PublicDevCloudDispathcType(
    @JsonProperty("value") var image: String?,
    override var imageType: ImageType? = ImageType.BKDEVOPS,
    override var credentialId: String? = "",
    override var credentialProject: String? = "",
    // 商店镜像代码
    override var imageCode: String? = "",
    // 商店镜像版本
    override var imageVersion: String? = "",
    // 商店镜像名称
    override var imageName: String? = ""
) : StoreDispatchType(if (image.isNullOrBlank())
    imageCode else image, DispatchRouteKeySuffix.DEVCLOUD, imageType, credentialId, credentialProject, imageCode, imageVersion, imageName) {
    override fun replaceField(variables: Map<String, String>) {
        image = EnvUtils.parseEnv(image!!, variables)
    }

    override fun buildType() = BuildType.valueOf(BuildType.PUBLIC_DEVCLOUD.name)
}