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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.dao.PipelineTaskDao
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.utils.PauseRedisUtils
import com.tencent.devops.process.pojo.PipelineProjectRel
import com.tencent.devops.process.websocket.page.DetailPageBuild
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKNAMES
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKS
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.pojo.common.PIPELINE_TASK_PAUSE_NOTIFY
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class PipelineTaskService @Autowired constructor(
    val dslContext: DSLContext,
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper,
    val pipelineTaskDao: PipelineTaskDao,
    val pipelineBuildDetailService: PipelineBuildDetailService,
    val pipelineModelTaskDao: PipelineModelTaskDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineVariableService: BuildVariableService,
    val pipelineInfoDao: PipelineInfoDao,
    val client: Client,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    fun list(projectId: String, pipelineIds: Collection<String>): Map<String, List<PipelineModelTask>> {
        return pipelineTaskDao.list(dslContext, projectId, pipelineIds)?.map {
            PipelineModelTask(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                stageId = it.stageId,
                containerId = it.containerId,
                taskId = it.taskId,
                taskSeq = it.taskSeq,
                taskName = it.taskName,
                atomCode = it.atomCode,
                classType = it.classType,
                taskAtom = it.taskAtom,
                taskParams = objectMapper.readValue(it.taskParams),
                additionalOptions = if (it.additionalOptions.isNullOrBlank())
                    null
                else objectMapper.readValue(it.additionalOptions, ElementAdditionalOptions::class.java),
                os = it.os
            )
        }?.groupBy { it.pipelineId } ?: mapOf()
    }

    fun list(pipelineIds: Collection<String>): Map<String, List<PipelineModelTask>> {
        return pipelineTaskDao.list(dslContext, pipelineIds)?.map {
            PipelineModelTask(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                stageId = it.stageId,
                containerId = it.containerId,
                taskId = it.taskId,
                taskSeq = it.taskSeq,
                taskName = it.taskName,
                atomCode = it.atomCode,
                classType = it.classType,
                taskAtom = it.taskAtom,
                taskParams = objectMapper.readValue(it.taskParams),
                additionalOptions = if (it.additionalOptions.isNullOrBlank()) null
                else objectMapper.readValue(it.additionalOptions, ElementAdditionalOptions::class.java),
                os = it.os
            )
        }?.groupBy { it.pipelineId } ?: mapOf()
    }

    /**
     * 根据插件标识，获取使用插件的流水线详情
     */
    fun listPipelinesByAtomCode(
        atomCode: String,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Page<PipelineProjectRel> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 100

        val count = pipelineModelTaskDao.getPipelineCountByAtomCode(dslContext, atomCode, projectCode).toLong()
        val pipelines =
            pipelineModelTaskDao.listByAtomCode(dslContext, atomCode, projectCode, pageNotNull, pageSizeNotNull)

        val pipelineAtomVersionInfo = mutableMapOf<String, MutableList<String>>()
        val pipelineIds = pipelines?.map { it["pipelineId"] as String }
        if (pipelineIds != null && pipelineIds.isNotEmpty()) {
            val pipelineAtoms = pipelineModelTaskDao.listByAtomCodeAndPipelineIds(dslContext, atomCode, pipelineIds)
            pipelineAtoms?.forEach {
                val pipelineId = it["pipelineId"] as String
                val taskParamsStr = it["taskParams"] as? String
                val taskParams = if (!taskParamsStr.isNullOrBlank()) JsonUtil.getObjectMapper()
                    .readValue(taskParamsStr, Map::class.java) as Map<String, Any> else mapOf()
                if (pipelineAtomVersionInfo.containsKey(pipelineId)) {
                    pipelineAtomVersionInfo[pipelineId]!!.add(taskParams["version"].toString())
                } else {
                    pipelineAtomVersionInfo[pipelineId] = mutableListOf(taskParams["version"].toString())
                }
            }
        }

        val records = if (pipelines == null) {
            listOf<PipelineProjectRel>()
        } else {
            pipelines.map {
                val pipelineId = it["pipelineId"] as String
                PipelineProjectRel(
                    pipelineId = pipelineId,
                    pipelineName = it["pipelineName"] as String,
                    projectCode = it["projectCode"] as String,
                    atomVersion = pipelineAtomVersionInfo.getOrDefault(pipelineId, mutableListOf<String>()).distinct()
                        .joinToString(",")
                )
            }
        }

        return Page(pageNotNull, pageSizeNotNull, count, records)
    }

    fun isRetryWhenFail(taskId: String, buildId: String): Boolean {
        val taskRecord = pipelineRuntimeService.getBuildTask(buildId, taskId)
        val retryCount = redisOperation.get(getRedisKey(taskRecord!!.buildId, taskRecord.taskId))?.toInt() ?: 0
        val isRry = ControlUtils.retryWhenFailure(taskRecord!!.additionalOptions, retryCount)
        if (isRry) {
            logger.info("retry task [$buildId]|stageId=${taskRecord.stageId}|container=${taskRecord.containerId}|taskId=$taskId|retryCount=$retryCount |vm atom will retry, even the task is failure")
            val nextCount = retryCount + 1
            redisOperation.set(getRedisKey(taskRecord!!.buildId, taskRecord.taskId), nextCount.toString())
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = "插件${taskRecord.taskName}执行失败, 5s后开始执行第${nextCount}次重试",
                tag = taskRecord.taskId,
                jobId = taskRecord.containerId,
                executeCount = 1
            )
        }
        return isRry
    }

    fun isPause(taskId: String, buildId: String): Boolean {
        val taskRecord = pipelineRuntimeService.getBuildTask(buildId, taskId)
        val pauseFlag = redisOperation.get(PauseRedisUtils.getPauseRedisKey(buildId, taskId))
        val isPause = ControlUtils.pauseBeforeExec(taskRecord!!.additionalOptions, pauseFlag)
        if (isPause) {
            logger.info("pause atom, buildId[$buildId], taskId[$taskId] , additionalOptions[${taskRecord!!.additionalOptions}]")
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = "【${taskRecord.taskName}】暂停中，等待人工处理...",
                tag = taskRecord.taskId,
                jobId = taskRecord.containerId,
                executeCount = 1
            )
            pauseBuild(
                buildId = buildId,
                stageId = taskRecord.stageId,
                taskId = taskRecord.taskId,
                containerId = taskRecord.containerId
            )

            try {
                // 发送消息给相关关注人
                val sendUser = taskRecord.additionalOptions!!.subscriptionPauseUser
                val subscriptionPauseUser = mutableSetOf<String>()
                if (sendUser != null) {
                    val sendUsers = sendUser.split(",")
                    subscriptionPauseUser.add(sendUsers.forEach { it }.toString())
                }
                sendPauseNotify(
                        buildId = buildId,
                        taskName = taskRecord.taskName,
                        pipelineId = taskRecord.pipelineId,
                        receivers = subscriptionPauseUser
                )
                logger.info("|$buildId| next task |$taskId| need pause, send End status to Vm agent")
            } catch (e: Exception) {
                logger.warn("pause atom send notify fail", e)
            }
        }
        return isPause
    }

    fun removeRetryCache(buildId: String, taskId: String) {
        // 清除该原子内的重试记录
        redisOperation.delete(getRedisKey(buildId, taskId))
    }

    fun createFailElementVar(buildId: String, projectId: String, pipelineId: String, taskId: String) {
        logger.info("$buildId| $taskId| atom fail, save var record")
        val taskRecord = pipelineRuntimeService.getBuildTask(buildId, taskId)
        val model = pipelineBuildDetailService.getBuildModel(buildId)
        val failTask = pipelineVariableService.getVariable(buildId, BK_CI_BUILD_FAIL_TASKS)
        val failTaskNames = pipelineVariableService.getVariable(buildId, BK_CI_BUILD_FAIL_TASKNAMES)
        var errorElements = ""
        var errorElementsName = ""
        if (taskRecord == null) {
            return
        }
        try {
            val errorElement = findElementMsg(model, taskRecord)
            errorElements = if (failTask.isNullOrBlank()) {
                errorElement.first
            } else {
                failTask + errorElement.first
            }

            errorElementsName = if (failTaskNames.isNullOrBlank()) {
                errorElement.second
            } else {
                "$failTaskNames,${errorElement.second}"
            }
            logger.info("$buildId| $taskId| atom fail record, tasks:$errorElement, taskNames:$errorElementsName")
            val valueMap = mutableMapOf<String, Any>()
            valueMap[BK_CI_BUILD_FAIL_TASKS] = errorElements
            valueMap[BK_CI_BUILD_FAIL_TASKNAMES] = errorElementsName
            pipelineVariableService.batchUpdateVariable(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                variables = valueMap
            )
        } catch (e: Exception) {
            logger.warn("createFailElementVar error, msg: $e")
        }
    }

    fun removeFailVarWhenSuccess(buildId: String, projectId: String, pipelineId: String, taskId: String) {
        val failTaskRecord = redisOperation.get(failTaskRedisKey(buildId, taskId))
        val failTaskNameRecord = redisOperation.get(failTaskNameRedisKey(buildId, taskId))
        if (failTaskRecord.isNullOrBlank() || failTaskNameRecord.isNullOrBlank()) {
            logger.info("$buildId|$taskId| retry fail, keep record")
            return
        }
        logger.info("$buildId|$taskId| retry success, start remove fail recode")
        try {
            val failTask = pipelineVariableService.getVariable(buildId, BK_CI_BUILD_FAIL_TASKS)
            val failTaskNames = pipelineVariableService.getVariable(buildId, BK_CI_BUILD_FAIL_TASKNAMES)
            failTask!!.replace(failTaskRecord!!, "")
            failTaskNames!!.replace(failTaskNameRecord!!, "")
            val valueMap = mutableMapOf<String, Any>()
            valueMap[BK_CI_BUILD_FAIL_TASKS] = failTask
            valueMap[BK_CI_BUILD_FAIL_TASKNAMES] = failTaskNames
            pipelineVariableService.batchUpdateVariable(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                variables = valueMap
            )
            redisOperation.delete(failTaskRedisKey(buildId, taskId))
            redisOperation.delete(failTaskNameRedisKey(buildId, taskId))
            logger.info("$buildId|$taskId| retry success, success remove fail recode")
        } catch (e: Exception) {
            logger.warn("removeFailVarWhenSuccess error, msg: $e")
        }
    }

    private fun findElementMsg(model: Model?, taskRecord: PipelineBuildTask): Pair<String, String> {
        var containerName = ""
        model?.stages?.forEach { stage ->
            if (stage.id == taskRecord.stageId) {
                stage.containers.forEach nextContainer@{ container ->
                    if (container.id == taskRecord.containerId) {
                        containerName = container.name
                        return@forEach
                    }
                }
            }
        }
        val failTask = "[${taskRecord.stageId}][$containerName]${taskRecord.taskName} \n"
        val failTaskName = "${taskRecord.taskName}"

        redisOperation.set(
            failTaskRedisKey(taskRecord.buildId, taskRecord.taskId),
            failTask,
            TimeUnit.DAYS.toMinutes(7L),
            true
        )
        redisOperation.set(
            failTaskNameRedisKey(taskRecord.buildId, taskRecord.taskId),
            failTaskName,
            TimeUnit.DAYS.toMinutes(7L),
            true
        )
        return Pair(failTask, failTaskName)
    }

    private fun failTaskRedisKey(buildId: String, taskId: String): String {
        return "devops:failTask:redis:key:$buildId:$taskId"
    }

    private fun failTaskNameRedisKey(buildId: String, taskId: String): String {
        return "devops:failTaskName:redis:key:$buildId:$taskId"
    }

    private fun getRedisKey(buildId: String, taskId: String): String {
        return "$retryCountRedisKey$buildId:$taskId"
    }

    fun pauseBuild(buildId: String, taskId: String, stageId: String, containerId: String) {
        logger.info("pauseBuild buildId[$buildId] stageId[$stageId] containerId[$containerId] taskId[$taskId]")
        // 修改任务状态位暂停
        pipelineRuntimeService.updateTaskStatus(
            buildId = buildId,
            taskId = taskId,
            userId = "",
            buildStatus = BuildStatus.PAUSE
        )

        logger.info("pauseBuild $buildId update task status success")

        pipelineBuildDetailService.pauseTask(
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            taskId = taskId,
            buildStatus = BuildStatus.PAUSE
        )
        logger.info("pauseBuild $buildId update detail status success")

        redisOperation.set(PauseRedisUtils.getPauseRedisKey(buildId, taskId), "true")
    }

    private fun sendPauseNotify(
        buildId: String,
        taskName: String,
        pipelineId: String,
        receivers: Set<String>?
    ) {
        val pipelineRecord = pipelineInfoDao.getPipelineInfo(dslContext, pipelineId)
        if (pipelineRecord == null) {
            logger.warn("sendPauseNotify pipeline[$pipelineId] is empty record")
            return
        }

        val buildRecord = pipelineRuntimeService.getBuildInfo(buildId)
        val pipelineName = (pipelineRecord?.pipelineName ?: "")
        val buildNum = buildRecord?.buildNum.toString()
        val projectName = client.get(ServiceProjectResource::class).get(pipelineRecord!!.projectId).data!!.projectName
        val url = DetailPageBuild().buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = pipelineRecord.projectId,
                atomId = null
            )
        )
        // 指定通过rtx发送
        val notifyType = mutableSetOf<String>()
        notifyType.add(NotifyType.RTX.name)

        // 若没有配置订阅人，则将暂停消息发送给发起人
        val receiver = mutableSetOf<String>()
        if (receivers == null || receivers.isEmpty()) {
            receiver.add(buildRecord!!.startUser)
            receiver.add(pipelineRecord.lastModifyUser)
        } else {
            receiver.addAll(receivers)
        }

        val msg = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_TASK_PAUSE_NOTIFY,
            titleParams = mapOf(
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum
            ),
            notifyType = notifyType,
            bodyParams = mapOf(
                "BK_CI_PROJECT_NAME_CN" to projectName,
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum,
                "taskName" to taskName,
                "BK_CI_START_USER_ID" to (buildRecord?.startUser ?: ""),
                "url" to url
            ),
            receivers = receiver
        )
        logger.info("sendPauseNotify|$buildId| $pipelineId| $msg")
        client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(msg)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        private const val retryCountRedisKey = "process:task:failRetry:count:"
    }
}