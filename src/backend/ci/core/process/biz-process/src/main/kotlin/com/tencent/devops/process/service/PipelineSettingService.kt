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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.PipelineAlreadyExistException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.event.PipelineSettingChangeEvent
import com.tencent.devops.model.process.tables.TPipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineSettingRecord
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineSettingService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineResDao: PipelineResDao,
    private val client: Client
) {

    fun saveSetting(userId: String, setting: PipelineSetting, checkPermission: Boolean = true): String {
        with(setting) {
            checkParam()
            checkEditPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                message = "用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)"
            )
        }

        if (isPipelineExist(projectId = setting.projectId, pipelineId = setting.pipelineId, name = setting.pipelineName)) {
            throw PipelineAlreadyExistException("流水线(${setting.pipelineName})已经存在")
        }
        pipelineGroupService.updatePipelineLabel(userId = userId, pipelineId = setting.pipelineId, labelIds = setting.labels)
        pipelineInfoDao.update(
            dslContext = dslContext,
            pipelineId = setting.pipelineId,
            userId = userId,
            updateVersion = false,
            pipelineName = setting.pipelineName,
            pipelineDesc = setting.desc
        )
        val id = pipelineSettingDao.saveSetting(dslContext, setting).toString()
        if (checkPermission) {
            pipelinePermissionService.modifyResource(projectId = setting.projectId, pipelineId = setting.pipelineId, pipelineName = setting.pipelineName)
        }
        // 流水线设置变更事件
        pipelineEventDispatcher.dispatch(
            PipelineSettingChangeEvent(
                source = "setting_change",
                pipelineName = setting.pipelineName,
                pipelineId = setting.pipelineId,
                projectId = setting.projectId,
                userId = userId
            )
        )
        return id
    }

    fun userGetSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode = ChannelCode.BS
    ): PipelineSetting {
        val setting = pipelineSettingDao.getSetting(dslContext, pipelineId)
        val groups = pipelineGroupService.getGroups(userId, projectId, pipelineId)
        val labels = ArrayList<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }
        return if (setting != null) {
            setting.map {
                with(TPipelineSetting.T_PIPELINE_SETTING) {
                    val successType = it.get(SUCCESS_TYPE).split(",").filter { i -> i.isNotBlank() }
                        .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                    val failType = it.get(FAIL_TYPE).split(",").filter { i -> i.isNotBlank() }
                        .map { type -> PipelineSubscriptionType.valueOf(type) }.toSet()
                    PipelineSetting(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineName = it.get(NAME),
                        desc = it.get(DESC),
                        runLockType = PipelineRunLockType.valueOf(it.get(RUN_LOCK_TYPE)),
                        successSubscription = Subscription(
                            successType,
                            it.get(SUCCESS_GROUP).split(",").toSet(),
                            it.get(SUCCESS_RECEIVER),
                            it.get(SUCCESS_WECHAT_GROUP_FLAG),
                            it.get(SUCCESS_WECHAT_GROUP),
                            it.get(SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG),
                            it.get(SUCCESS_DETAIL_FLAG),
                            it.get(SUCCESS_CONTENT) ?: ""
                        ),
                        failSubscription = Subscription(
                            failType,
                            it.get(FAIL_GROUP).split(",").toSet(),
                            it.get(FAIL_RECEIVER),
                            it.get(FAIL_WECHAT_GROUP_FLAG),
                            it.get(FAIL_WECHAT_GROUP),
                            it.get(FAIL_WECHAT_GROUP_MARKDOWN_FLAG),
                            it.get(FAIL_DETAIL_FLAG),
                            it.get(FAIL_CONTENT) ?: ""
                        ),
                        labels = labels,
                        waitQueueTimeMinute = DateTimeUtil.secondToMinute(it.get(WAIT_QUEUE_TIME_SECOND)),
                        maxQueueSize = it.get(MAX_QUEUE_SIZE),
                        maxPipelineResNum = it.get(MAX_PIPELINE_RES_NUM),
                        maxConRunningQueueSize = it.get(MAX_CON_RUNNING_QUEUE_SIZE)
                    )
                }
            }
        } else {
            val model = client.get(ServicePipelineResource::class).get(userId, projectId, pipelineId, channelCode).data
            val name = model?.name ?: "unknown pipeline name"
            val desc = model?.desc ?: ""
            PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = name,
                desc = desc,
                runLockType = PipelineRunLockType.MULTIPLE,
                successSubscription = Subscription(),
                failSubscription = Subscription(),
                labels = labels
            )
        }
    }

    fun getSetting(pipelineId: String): TPipelineSettingRecord? {
        return pipelineSettingDao.getSetting(dslContext, pipelineId)
    }

    private fun isPipelineExist(
        projectId: String,
        pipelineId: String,
        name: String,
        channelCode: ChannelCode = ChannelCode.BS
    ): Boolean {
        return pipelineInfoDao.isNameExist(dslContext, projectId, name, channelCode, pipelineId)
    }

    private fun checkEditPermission(userId: String, projectId: String, pipelineId: String, message: String) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        ) {
            throw PermissionForbiddenException(message)
        }
    }

    fun isQueueTimeout(pipelineId: String, startTime: Long): Boolean {
        val waitQueueTimeMills = (getSetting(pipelineId)?.waitQueueTimeSecond ?: 3600) * 1000
        return System.currentTimeMillis() - startTime > waitQueueTimeMills
    }

    fun updatePipelineModel(
        userId: String,
        updatePipelineModelRequest: UpdatePipelineModelRequest,
        checkPermission: Boolean = true
    ): Boolean {
        val pipelineModelVersionList = updatePipelineModelRequest.pipelineModelVersionList
        if (checkPermission) {
            pipelineModelVersionList.forEach {
                checkEditPermission(
                    userId = it.creator,
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    message = "The user (\$ userId) does not have permission to edit the pipeline (\$ pipelineId) under the project (\$ projectId)"
                )
            }
        }
        pipelineResDao.updatePipelineModel(
            dslContext = dslContext,
            userId = userId,
            pipelineModelVersionList = pipelineModelVersionList
        )
        return true
    }

    fun maxQueue(pipelineId: String): Int {
        return getSetting(pipelineId)?.maxQueueSize ?: 10
    }

    fun PipelineSetting.checkParam() {
        if (maxPipelineResNum < 1) {
            throw InvalidParamException(message = "流水线编排数量非法", params = arrayOf("maxPipelineResNum"))
        }
        if (runLockType == PipelineRunLockType.SINGLE || runLockType == PipelineRunLockType.SINGLE_LOCK) {
            if (waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN || waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX) {
                throw InvalidParamException("最大排队时长非法", params = arrayOf("waitQueueTimeMinute"))
            }
            if (maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN || maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX) {
                throw InvalidParamException("最大排队数量非法", params = arrayOf("maxQueueSize"))
            }
        }
        if (maxConRunningQueueSize <= PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN || maxConRunningQueueSize > PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX) {
            throw InvalidParamException("最大并发数量非法", params = arrayOf("maxConRunningQueueSize"))
        }
    }
}
