/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.archive.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 仓库配置
 */
@Component
class BkRepoClientConfig {

    @Value("\${artifactory.realm:}")
    lateinit var artifactoryRealm: String

    @Value("\${bkrepo.logRepoCredentialsKey:}")
    lateinit var logRepoCredentialsKey: String

    // 蓝盾新仓库api接口地址
    @Value("\${bkrepo.bkrepoApiUrl:}")
    val bkRepoApiUrl: String = ""

    // 蓝盾新仓库静态资源仓库前缀地址
    @Value("\${bkrepo.staticRepoPrefixUrl:}")
    val bkRepoStaticRepoPrefixUrl: String = ""

    // 蓝盾新仓库静态资源仓库用户名
    @Value("\${bkrepo.staticUserName:g_bkstore}")
    val bkRepoStaticUserName: String = "g_bkstore"

    // 蓝盾新仓库静态资源仓库密码
    @Value("\${bkrepo.staticPassword:}")
    val bkRepoStaticPassword: String = ""

    @Value("\${bkrepo.bkrepoUrl:}")
    val bkRepoIdcHost: String = ""

    @Value("\${bkrepo.devxIdcBkrepoUrl:}")
    val bkRepoDevxIdcHost: String = ""

    /**
     * bkrepo 服务的网关入口 URL (BK_REPO_PRIVATE_URL)。
     *
     * BkRepoClient 拼接 `/bkrepo/api/{service|build|user|external}/...` 时使用。
     *
     * 与 [com.tencent.devops.common.service.config.CommonConfig.devopsIdcGateway] (BK_CI_PRIVATE_URL)
     * 解耦后, 可单独配置, 避免在 "外部网关 + 子路径" 拓扑下把 BK-CI 自己的子路径前缀错误地拼到 bkrepo 调用上。
     *
     * 默认空, 此时 BkRepoClient 回退到 devopsIdcGateway, 与历史共享网关模型保持一致。
     */
    @Value("\${bkrepo.gatewayUrl:}")
    val bkrepoGatewayUrl: String = ""
}
