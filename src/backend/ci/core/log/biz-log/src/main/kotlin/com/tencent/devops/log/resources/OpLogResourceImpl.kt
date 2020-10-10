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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.OpLogResource
import com.tencent.devops.log.cron.CleanBuildJob
import com.tencent.devops.log.cron.ESIndexCloseJob
import com.tencent.devops.log.service.LogService
import org.springframework.beans.factory.annotation.Autowired

/**
 *
 * Powered By Tencent
 */
@RestResource
class OpLogResourceImpl @Autowired constructor(
    private val esIndexCloseJob: ESIndexCloseJob,
    private val cleanBuildJob: CleanBuildJob,
    private val logService: LogService
) : OpLogResource {

    override fun getBuildExpire(): Result<Int> {
        return Result(cleanBuildJob.getExpire())
    }

    override fun setBuildExpire(expire: Int): Result<Boolean> {
        cleanBuildJob.expire(expire)
        return Result(true)
    }

    override fun getESExpire(): Result<Int> {
        return Result(esIndexCloseJob.getExpireIndexDay())
    }

    override fun setESExpire(expire: Int): Result<Boolean> {
        esIndexCloseJob.updateExpireIndexDay(expire)
        return Result(true)
    }

    override fun reopenIndex(buildId: String): Result<Boolean> {
        return Result(logService.reopenIndex(buildId))
    }
}