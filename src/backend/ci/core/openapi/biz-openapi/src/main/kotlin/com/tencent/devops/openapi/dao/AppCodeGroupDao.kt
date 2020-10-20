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

package com.tencent.devops.openapi.dao

import com.tencent.devops.model.openapi.tables.TAppCodeGroup
import com.tencent.devops.model.openapi.tables.records.TAppCodeGroupRecord
import com.tencent.devops.openapi.pojo.AppCodeGroup
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AppCodeGroupDao {
    fun set(
        dslContext: DSLContext,
        userName: String,
        appCodeGroup: AppCodeGroup
    ): Boolean {
        val exist = existByAppCodeGroup(dslContext, appCodeGroup)
        val now = LocalDateTime.now()
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return if (exist) {
                true
            } else {
                dslContext.insertInto(this,
                    APP_CODE,
                    BG_ID,
                    BG_NAME,
                    DEPT_ID,
                    DEPT_NAME,
                    CENTER_ID,
                    CENTER_NAME,
                    CREATOR,
                    CREATE_TIME,
                    UPDATER,
                    UPDATE_TIME
                ).values(
                    appCodeGroup.appCode,
                    appCodeGroup.bgId,
                    appCodeGroup.bgName,
                    appCodeGroup.deptId,
                    appCodeGroup.deptName,
                    appCodeGroup.centerId,
                    appCodeGroup.centerName,
                    userName,
                    now,
                    userName,
                    now
                ).execute() > 0
            }
        }
    }

    fun update(
            dslContext: DSLContext,
            userName: String,
            appCodeGroupId: Long,
            appCodeGroup: AppCodeGroup
    ): Boolean {
        val exist = existById(dslContext, appCodeGroupId)
        val now = LocalDateTime.now()
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return if (exist) {
                dslContext.update(this)
                        .set(APP_CODE, appCodeGroup.appCode)
                        .set(BG_ID, appCodeGroup.bgId)
                        .set(BG_NAME, appCodeGroup.bgName)
                        .set(DEPT_ID, appCodeGroup.deptId)
                        .set(DEPT_NAME, appCodeGroup.deptName)
                        .set(CENTER_ID, appCodeGroup.centerId)
                        .set(CENTER_NAME, appCodeGroup.centerName)
                        .set(UPDATER, userName)
                        .set(UPDATE_TIME, now)
                        .where(ID.eq(appCodeGroupId))
                        .execute() > 0
            } else {
                false
            }
        }

    }

    fun get(
            dslContext: DSLContext,
            appCodeGroupId: Long
    ): TAppCodeGroupRecord? {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(appCodeGroupId))
                    .fetchOne()
        }
    }

    fun getListByAppCode(
            dslContext: DSLContext,
            appCode: String
    ): Result<TAppCodeGroupRecord> {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.selectFrom(this)
                    .where(APP_CODE.eq(appCode)).fetch()
        }
    }

    fun list(
        dslContext: DSLContext
    ): Result<TAppCodeGroupRecord> {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.selectFrom(this).fetch()
        }
    }

    fun existByAppCodeGroup(
        dslContext: DSLContext,
        appCodeGroup: AppCodeGroup
    ): Boolean {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.select()
                .from(this)
                .where(APP_CODE.eq(appCodeGroup.appCode)
                        .and(BG_ID.eq(appCodeGroup.bgId))
                        .and(DEPT_ID.eq(appCodeGroup.deptId))
                        .and(CENTER_ID.eq(appCodeGroup.centerId))
                ).fetch().isNotEmpty
        }
    }

    fun existById(
            dslContext: DSLContext,
            appCodeGroupId: Long
    ): Boolean {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.select()
                    .from(this)
                    .where(ID.eq(appCodeGroupId)).fetch().isNotEmpty
        }
    }

    fun delete(
        dslContext: DSLContext,
        appCodeGroupId: Long
    ): Boolean {
        with(TAppCodeGroup.T_APP_CODE_GROUP) {
            return dslContext.delete(this)
                .where(ID.eq(appCodeGroupId)).execute() > 0
        }
    }
}