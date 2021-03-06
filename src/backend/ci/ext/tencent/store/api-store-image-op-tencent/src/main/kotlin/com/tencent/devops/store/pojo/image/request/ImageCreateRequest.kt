package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import io.swagger.annotations.ApiModelProperty

data class ImageCreateRequest(
    @ApiModelProperty("项目编码", required = true)
    val projectCode: String,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("镜像标识", required = true)
    val imageCode: String,
    @ApiModelProperty("所属分类代码", required = true)
    val classifyCode: String,
    @ApiModelProperty("镜像所属范畴CATEGORY_CODE", required = false)
    val category: String?,
    @ApiModelProperty("镜像适用的构建机类型", required = true)
    val agentTypeScope: List<ImageAgentTypeEnum>,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty(
        "发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正",
        required = true
    )
    val releaseType: ReleaseTypeEnum,
    @ApiModelProperty("版本日志内容", required = true)
    val versionContent: String,
    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: ImageType,
    @ApiModelProperty("镜像仓库地址", required = false)
    val imageRepoUrl: String?,
    @ApiModelProperty("镜像在仓库中的名称", required = true)
    val imageRepoName: String,
    @ApiModelProperty("凭证ID", required = false)
    val ticketId: String?,
    @ApiModelProperty("镜像TAG", required = true)
    val imageTag: String,
    @ApiModelProperty("dockerFile类型", required = true)
    val dockerFileType: String?,
    @ApiModelProperty("dockerFile内容", required = true)
    val dockerFileContent: String?,
    @ApiModelProperty("logo地址", required = false)
    val logoUrl: String?,
    @ApiModelProperty("镜像简介", required = false)
    val summary: String?,
    @ApiModelProperty("镜像描述", required = false)
    val description: String?,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("镜像标签列表", required = false)
    val labelIdList: ArrayList<String>?
)
