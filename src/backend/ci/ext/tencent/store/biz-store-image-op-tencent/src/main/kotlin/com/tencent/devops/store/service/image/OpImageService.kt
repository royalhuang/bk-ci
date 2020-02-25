package com.tencent.devops.store.service.image

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.dao.OpImageDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.image.Constants
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_WEIGHT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LATEST_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_RD_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ID
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_TYPE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.KEY_VERSION_LOG_CONTENT
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.CategoryTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import com.tencent.devops.store.pojo.image.request.ImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.request.OpImageSortTypeEnum
import com.tencent.devops.store.pojo.image.response.OpImageItem
import com.tencent.devops.store.pojo.image.response.OpImageResp
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpImageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val marketImageDao: MarketImageDao,
    private val opImageDao: OpImageDao,
    private val imageLabelService: ImageLabelService,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val imageFeatureDao: ImageFeatureDao,
    private val imageAgentTypeDao: ImageAgentTypeDao,
    private val imageCategoryRelDao: ImageCategoryRelDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val imageService: ImageService,
    private val imageNotifyService: ImageNotifyService,
    private val imageReleaseService: ImageReleaseService
) {

    private val logger = LoggerFactory.getLogger(MarketImageService::class.java)

    fun addImage(
        accessToken: String,
        userId: String,
        imageCreateRequest: ImageCreateRequest,
        checkLatest: Boolean = true,
        needAuth: Boolean = true,
        sendCheckResultNotify: Boolean = true,
        runCheckPipeline: Boolean = true
    ): Result<String> {
        logger.info("addImage accessToken is :$accessToken, userId is :$userId, imageCreateRequest is :$imageCreateRequest")
        val imageCode = imageCreateRequest.imageCode
        val imageRecords = marketImageDao.getImagesByImageCode(dslContext, imageCode)
        logger.info("the imageRecords is :$imageRecords")
        val imageName = imageCreateRequest.imageName
        val imageSourceType = imageCreateRequest.imageSourceType
        val ticketId = imageCreateRequest.ticketId
        // 判断是不是首次新增镜像
        val imageId = if (null == imageRecords || imageRecords.size == 0) {
            val addImageResult = imageReleaseService.addMarketImage(
                accessToken = accessToken,
                userId = userId,
                imageCode = imageCode,
                marketImageRelRequest = MarketImageRelRequest(
                    projectCode = imageCreateRequest.projectCode,
                    imageName = imageName,
                    imageSourceType = imageSourceType,
                    ticketId = ticketId
                ),
                needAuth = needAuth
            )
            logger.info("addImageResult is :$addImageResult")
            if (addImageResult.isNotOk() || addImageResult.data.isNullOrBlank()) {
                return addImageResult
            }
            addImageResult.data!!
        } else {
            imageRecords[0].id!!
        }
        // 更新镜像信息
        val updateImageResult = imageReleaseService.updateMarketImage(
            userId = userId,
            marketImageUpdateRequest = MarketImageUpdateRequest(
                imageCode = imageCode,
                imageName = imageName,
                classifyCode = imageCreateRequest.classifyCode,
                labelIdList = imageCreateRequest.labelIdList,
                category = imageCreateRequest.category,
                agentTypeScope = imageCreateRequest.agentTypeScope,
                summary = imageCreateRequest.summary,
                description = imageCreateRequest.description,
                logoUrl = imageCreateRequest.logoUrl,
                ticketId = imageCreateRequest.ticketId,
                imageSourceType = imageSourceType,
                imageRepoUrl = imageCreateRequest.imageRepoUrl,
                imageRepoName = imageCreateRequest.imageRepoName,
                imageTag = imageCreateRequest.imageTag,
                dockerFileType = imageCreateRequest.dockerFileType,
                dockerFileContent = imageCreateRequest.dockerFileContent,
                version = imageCreateRequest.version,
                releaseType = imageCreateRequest.releaseType,
                versionContent = imageCreateRequest.versionContent,
                publisher = imageCreateRequest.publisher
            ),
            checkLatest = checkLatest,
            sendCheckResultNotify = sendCheckResultNotify,
            runCheckPipeline = runCheckPipeline
        )
        logger.info("updateImageResult is :$updateImageResult")
        if (updateImageResult.isNotOk()) {
            return Result(
                status = updateImageResult.status,
                message = updateImageResult.message,
                data = null
            )
        }
        return Result(data = imageId)
    }

    fun update(
        userId: String,
        imageId: String,
        imageUpdateRequest: ImageUpdateRequest,
        interfaceName: String? = "Anon interface"
    ): Result<Boolean> {
        val imageRecord = imageDao.getImage(dslContext, imageId) ?: throw ImageNotExistException("imageId=$imageId")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val imageSize = try {
                imageUpdateRequest.imageSize?.toInt()
            } catch (ignore: Exception) {
                null
            }
            imageDao.updateImage(
                dslContext = context,
                imageId = imageId,
                imageUpdateBean = ImageDao.ImageUpdateBean(
                    imageName = imageUpdateRequest.imageName,
                    classifyId = imageUpdateRequest.classifyId,
                    version = imageUpdateRequest.version,
                    imageSourceType = imageUpdateRequest.imageSourceType,
                    imageRepoUrl = imageUpdateRequest.imageRepoUrl,
                    imageRepoName = imageUpdateRequest.imageRepoName,
                    ticketId = imageUpdateRequest.ticketId,
                    imageStatus = null,
                    imageStatusMsg = null,
                    imageSize = imageSize?.toString(),
                    imageTag = imageUpdateRequest.imageTag,
                    dockerFileType = imageUpdateRequest.dockerFileType,
                    dockerFileContent = imageUpdateRequest.dockerFileContent,
                    agentTypeList = imageUpdateRequest.agentTypeScope,
                    logoUrl = imageUpdateRequest.logoUrl,
                    icon = imageUpdateRequest.icon,
                    summary = imageUpdateRequest.summary,
                    description = imageUpdateRequest.description,
                    publisher = imageUpdateRequest.publisher,
                    // 是否为最新版本镜像只走发布和下架逻辑更新
                    latestFlag = null,
                    modifier = userId
                )
            )
            imageFeatureDao.update(
                dslContext = context,
                imageCode = imageRecord.imageCode,
                publicFlag = imageUpdateRequest.publicFlag,
                recommendFlag = imageUpdateRequest.recommendFlag,
                certificationFlag = imageUpdateRequest.certificationFlag,
                rdType = imageUpdateRequest.rdType,
                weight = imageUpdateRequest.weight,
                modifier = userId
            )
            // 更新调试项目
            val projectCode = imageUpdateRequest.projectCode
            if (projectCode != null) {
                storeProjectRelDao.updateUserStoreTestProject(dslContext, userId, projectCode, StoreProjectTypeEnum.TEST, imageRecord.imageCode, StoreTypeEnum.IMAGE)
            }
            // 若更新已发布、下架中、已下架版本，需要更新最终agentType
            if (imageRecord.imageStatus in setOf(ImageStatusEnum.RELEASED, ImageStatusEnum.UNDERCARRIAGING, ImageStatusEnum.UNDERCARRIAGED).map { it.status.toByte() }) {
                saveImageAgentTypeToFeature(dslContext, imageRecord.imageCode, imageUpdateRequest.agentTypeScope)
            }
            // 更新范畴
            val categoryIdList = imageUpdateRequest.categoryIdList
            if (categoryIdList != null) {
                imageService.saveImageCategoryByIds(
                    context = context,
                    userId = userId,
                    imageId = imageId,
                    categoryIdList = categoryIdList
                )
            }
            // 更新标签
            if (imageUpdateRequest.labelIdList != null) {
                imageLabelService.updateImageLabels(
                    dslContext = dslContext,
                    userId = userId,
                    imageId = imageId,
                    labelIdList = imageUpdateRequest.labelIdList!!
                )
            }
        }
        return Result(true)
    }

    fun list(
        userId: String,
        imageName: String?,
        imageSourceType: ImageType?,
        // 是否处于流程中
        processFlag: Boolean?,
        classifyCode: String?,
        categoryCodes: Set<String>?,
        labelCodes: Set<String>?,
        sortType: OpImageSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "Anon interface"
    ): Result<OpImageResp> {
        logger.info("$interfaceName:list:Input($userId,$imageName,$imageSourceType,$processFlag,$classifyCode,${categoryCodes?.size}$categoryCodes,${labelCodes?.size}$labelCodes,$sortType,$desc,$page,$pageSize)")
        val validPage = PageUtil.getValidPage(page)
        val validPageSize = pageSize ?: -1

        val images = opImageDao.listOpImages(
            dslContext = dslContext,
            imageName = imageName,
            imageType = imageSourceType,
            classifyCode = classifyCode,
            categoryCodeList = categoryCodes,
            labelCodeList = labelCodes,
            processFlag = processFlag,
            sortType = sortType,
            desc = desc,
            page = validPage,
            pageSize = validPageSize,
            interfaceName = interfaceName
        )

        val count = opImageDao.countOpImages(
            dslContext = dslContext,
            imageName = imageName,
            imageType = imageSourceType,
            classifyCode = classifyCode,
            categoryCodeList = categoryCodes,
            labelCodeList = labelCodes,
            processFlag = processFlag
        )
        val records = images?.map {
            val imageId = it.get(Constants.KEY_IMAGE_ID) as String
            val imageCode = it.get(KEY_IMAGE_CODE) as String
            val categoryList = imageCategoryRelDao.getCategorysByImageId(dslContext, imageId)?.map { category ->
                Category(
                    category.get(KEY_CATEGORY_ID) as String,
                    category.get(KEY_CATEGORY_CODE) as String,
                    category.get(KEY_CATEGORY_NAME) as String,
                    HashMap(),
                    category.get(KEY_CATEGORY_ICON_URL) as String,
                    CategoryTypeEnum.getCategoryType((category.get(KEY_CATEGORY_TYPE) as Byte).toInt()),
                    (category.get(KEY_CREATE_TIME) as LocalDateTime).timestampmilli(),
                    (category.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
                )
            } ?: emptyList()
            var newestImageRecord = marketImageDao.getNewestImageByCode(dslContext, imageCode)
            // 查询镜像最新的一条记录是否需要管理员介入
            val imageFinalStatusList = listOf(
                ImageStatusEnum.INIT.status.toByte(),
                ImageStatusEnum.COMMITTING.status.toByte(),
                ImageStatusEnum.CHECKING.status.toByte(),
                ImageStatusEnum.CHECK_FAIL.status.toByte(),
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.AUDITING.status.toByte()
            )
            if (null == newestImageRecord || !imageFinalStatusList.contains(newestImageRecord.imageStatus)) {
                newestImageRecord = null
            }
            val category = if (categoryList.isNotEmpty()) {
                categoryList[0]
            } else {
                null
            }
            val agentTypeScope = imageAgentTypeDao.getAgentTypeByImageCode(dslContext, imageCode)?.map {
                ImageAgentTypeEnum.getImageAgentType(it.get(Constants.KEY_IMAGE_AGENT_TYPE) as String)!!
            } ?: emptyList()
            val rdTypeRecord = it.get(KEY_IMAGE_RD_TYPE) as Byte?
            val rdTypeStr = if (rdTypeRecord == null) {
                ImageRDTypeEnum.THIRD_PARTY.name
            } else {
                ImageRDTypeEnum.getImageRDTypeStr(rdTypeRecord.toInt())
            }
            OpImageItem(
                imageId = imageId,
                imageCode = imageCode,
                imageName = it.get(KEY_IMAGE_NAME) as String,
                rdType = rdTypeStr,
                agentTypeScope = agentTypeScope,
                imageType = ImageType.getType(it.get(KEY_IMAGE_SOURCE_TYPE) as String),
                imageVersion = it.get(KEY_IMAGE_VERSION) as String,
                imageStatus = ImageStatusEnum.getImageStatus((it.get(KEY_IMAGE_STATUS) as Byte).toInt()),
                opImageId = newestImageRecord?.id,
                opImageVersion = newestImageRecord?.version,
                opImageStatus = if (null != newestImageRecord) ImageStatusEnum.getImageStatus(
                    newestImageRecord.imageStatus?.toInt() ?: 0
                ) else null,
                classifyCode = it.get(KEY_CLASSIFY_CODE) as String,
                classifyName = it.get(KEY_CLASSIFY_NAME) as String,
                category = category?.categoryCode ?: "",
                categoryName = category?.categoryName ?: "",
                publisher = it.get(KEY_PUBLISHER) as String?,
                pubTime = (it.get(KEY_PUB_TIME) as LocalDateTime?)?.timestampmilli(),
                pubDescription = it.get(KEY_VERSION_LOG_CONTENT) as String?,
                latestFlag = it.get(KEY_IMAGE_LATEST_FLAG) as Boolean,
                publicFlag = it.get(KEY_IMAGE_FEATURE_PUBLIC_FLAG) as Boolean,
                recommendFlag = it.get(KEY_IMAGE_FEATURE_RECOMMEND_FLAG) as Boolean,
                weight = (it.get(KEY_IMAGE_FEATURE_WEIGHT) as Int?) ?: 0,
                creator = it.get(KEY_CREATOR) as String?,
                modifier = it.get(KEY_MODIFIER) as String?,
                createTime = (it.get(KEY_CREATE_TIME) as LocalDateTime).timestampmilli(),
                updateTime = (it.get(KEY_UPDATE_TIME) as LocalDateTime).timestampmilli()
            )
        } ?: emptyList<OpImageItem>()

        logger.info("$interfaceName:list:Output:OpImageResp($count,$validPage,$validPageSize,records.size=${records.size})")
        return Result(
            OpImageResp(
                count = count,
                page = validPage,
                pageSize = validPageSize,
                records = records
            )
        )
    }

    /**
     * 审核镜像
     */
    fun approveImageWithoutNotify(userId: String, imageId: String, approveImageReq: ApproveImageReq, checkCurrentStatus: Boolean = true): AuditTypeEnum {
        // 参数校验
        // 判断镜像是否存在
        val image = imageDao.getImage(dslContext, imageId)
            ?: throw InvalidParamException(
                message = "imageId=$imageId is not valid",
                params = arrayOf(imageId)
            )
        val oldStatus = image.imageStatus
        // 非待审核状态直接返回
        if (checkCurrentStatus && oldStatus != ImageStatusEnum.AUDITING.status.toByte()) {
            throw InvalidParamException(
                message = "imageId=$imageId is not in approving state",
                params = arrayOf(imageId)
            )
        }
        // 审核结果校验
        val approveResult = approveImageReq.result
        if (approveResult != PASS && approveResult != REJECT) {
            throw InvalidParamException(
                message = "approveResult=$approveResult is not valid,should be PASS/REJECT",
                params = arrayOf(approveResult)
            )
        }
        val imageStatus =
            if (approveResult == PASS) {
                ImageStatusEnum.RELEASED.status.toByte()
            } else {
                ImageStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val type = if (approveResult == PASS) AuditTypeEnum.AUDIT_SUCCESS else AuditTypeEnum.AUDIT_REJECT
        val imageStatusMsg = approveImageReq.message
        dslContext.transaction { t ->
            val context = DSL.using(t)
            handleImageRelease(
                context = context,
                userId = userId,
                approveResult = approveResult,
                image = image,
                imageStatus = imageStatus,
                imageStatusMsg = imageStatusMsg,
                publicFlag = approveImageReq.publicFlag,
                recommendFlag = approveImageReq.recommendFlag,
                certificationFlag = approveImageReq.certificationFlag,
                rdType = approveImageReq.rdType,
                weight = approveImageReq.weight
            )
        }
        return type
    }

    fun approveImage(userId: String, imageId: String, approveImageReq: ApproveImageReq): Result<Boolean> {
        val type = approveImageWithoutNotify(
            userId = userId,
            imageId = imageId,
            approveImageReq = approveImageReq
        )
        // 通知发布者
        imageNotifyService.sendImageReleaseAuditNotifyMessage(imageId, type)
        return Result(true)
    }

    private fun saveImageAgentTypeToFeature(
        context: DSLContext,
        imageCode: String,
        agentTypeList: List<ImageAgentTypeEnum>
    ) {
        imageAgentTypeDao.deleteAgentTypeByImageCode(context, imageCode)
        agentTypeList.forEach {
            imageAgentTypeDao.addAgentTypeByImageCode(context, imageCode, it)
        }
    }

    fun handleImageRelease(
        context: DSLContext,
        userId: String,
        approveResult: String,
        image: TImageRecord,
        imageStatus: Byte,
        imageStatusMsg: String,
        publicFlag: Boolean,
        recommendFlag: Boolean,
        certificationFlag: Boolean,
        rdType: ImageRDTypeEnum?,
        weight: Int?
    ) {
        val latestFlag = approveResult == PASS
        var pubTime: LocalDateTime? = null
        if (latestFlag) {
            // 清空旧版本LATEST_FLAG
            marketImageDao.cleanLatestFlag(context, image.imageCode)
            pubTime = LocalDateTime.now()
            // 记录发布信息
            storeReleaseDao.addStoreReleaseInfo(
                dslContext = context,
                userId = userId,
                storeReleaseCreateRequest = StoreReleaseCreateRequest(
                    storeCode = image.imageCode,
                    storeType = StoreTypeEnum.IMAGE,
                    latestUpgrader = image.creator,
                    latestUpgradeTime = pubTime
                )
            )
            imageFeatureDao.update(
                context,
                image.imageCode,
                publicFlag,
                recommendFlag,
                certificationFlag,
                rdType,
                userId,
                weight
            )
        }
        marketImageDao.updateImageStatusInfo(
            dslContext = context,
            imageId = image.id,
            imageStatus = imageStatus,
            imageStatusMsg = imageStatusMsg,
            latestFlag = latestFlag,
            pubTime = pubTime
        )
        saveImageAgentTypeToFeature(
            context,
            image.imageCode,
            JsonUtil.to(image.agentTypeScope, object : TypeReference<List<ImageAgentTypeEnum>>() {})
        )
    }

    fun releaseImageDirectly(
        context: DSLContext,
        userId: String,
        imageCode: String,
        imageId: String
    ) {
        approveImageWithoutNotify(
            userId = userId,
            imageId = imageId,
            approveImageReq = ApproveImageReq(
                imageCode = imageCode,
                publicFlag = false,
                recommendFlag = true,
                certificationFlag = false,
                rdType = ImageRDTypeEnum.THIRD_PARTY,
                weight = 1,
                result = PASS,
                message = "historyData数据迁移自动通过"
            ),
            checkCurrentStatus = false
        )
    }
}