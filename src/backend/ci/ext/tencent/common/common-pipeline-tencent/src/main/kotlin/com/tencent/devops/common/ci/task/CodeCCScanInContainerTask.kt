package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(V3插件)")
open class CodeCCScanInContainerTask(
    @ApiModelProperty("id", required = false)
    override var displayName: String,
    @ApiModelProperty("入参", required = true)
    override val inputs: CodeCCScanInContainerInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {

    companion object {
        const val taskType = "codeCCScan"
        const val taskVersion = "@latest"
        const val atomCode = "CodeccCheckAtomDebug"
    }

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
                "CodeCC扫描",
                null,
                null,
                atomCode,
                "4.*",
                mapOf("input" to inputs)
        )
    }
}

@ApiModel("CodeCC代码检查任务(V3插件)")
data class CodeCCScanInContainerInput(
    @ApiModelProperty("语言", required = true)
    val languages: List<String>? = null, // ["PYTHON", "KOTLIN"]
    @ApiModelProperty("工具", required = true)
    val tools: List<String>? = null, // ["PYTHON", "KOTLIN"]
    @ApiModelProperty("白名单", required = false)
    var path: List<String>? = null,
    @ApiModelProperty("编译脚本", required = false)
    val script: String? = null,
    @ApiModelProperty("规则集", required = true)
    val languageRuleSetMap: Map<String, List<String>?>? = emptyMap(),
    @ApiModelProperty("全量还是增量, 1：增量；0：全量", required = false)
    val toolScanType: String? = null, // 对应接口的scanType, 1：增量；0：全量 2: diff模式
    @ApiModelProperty("黑名单，添加后的代码路径将不会产生告警", required = false)
    val customPath: String? = null // 黑名单，添加后的代码路径将不会产生告警
) : AbstractInput()
