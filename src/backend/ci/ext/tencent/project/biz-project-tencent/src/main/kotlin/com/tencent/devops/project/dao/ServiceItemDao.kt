package com.tencent.devops.project.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TServiceItem
import com.tencent.devops.model.project.tables.records.TServiceItemRecord
import com.tencent.devops.project.pojo.ItemCreateInfo
import com.tencent.devops.project.pojo.ItemQueryInfo
import com.tencent.devops.project.pojo.ItemUpdateInfo
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ServiceItemDao {

    fun add(dslContext: DSLContext, userId: String, info: ItemCreateInfo) {
        with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.insertInto(this,
                ID,
                ITEM_CODE,
                ITEM_NAME,
                PARENT_ID,
                HTML_COMPONENT_TYPE,
                HTML_PATH,
                CREATOR,
                CREATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    info.itemCode,
                    info.itemName,
                    info.pid,
                    info.UIType,
                    info.htmlPath,
                    userId,
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun update(dslContext: DSLContext, itemId: String, userId: String, info: ItemUpdateInfo) {
        with(TServiceItem.T_SERVICE_ITEM) {
            val baseStep = dslContext.update(this)
            if(null != info.itemName){
                baseStep.set(ITEM_NAME, info.itemName)
            }
            if(null != info.htmlPath){
                baseStep.set(HTML_PATH, info.htmlPath)
            }
            if(null != info.pid){
                baseStep.set(PARENT_ID, info.pid)
            }
            if(null != info.UIType){
                baseStep.set(HTML_COMPONENT_TYPE, info.UIType)
            }
            baseStep.set(MODIFIER, userId)
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(itemId))
                .execute()
        }
    }

    fun queryItem(dslContext: DSLContext, itemQueryInfo: ItemQueryInfo): Result<TServiceItemRecord?>{
        return with(TServiceItem.T_SERVICE_ITEM) {
            val whereStep = dslContext.selectFrom(this)
            if(itemQueryInfo.itemName != null){
                whereStep.where(ITEM_NAME.like(itemQueryInfo.itemName))
            }

            if(itemQueryInfo.pid != null){
                whereStep.where(PARENT_ID.eq(itemQueryInfo.pid))
            }
            whereStep.orderBy(UPDATE_TIME).fetch()
        }
    }

    fun getItemById(dslContext: DSLContext, itemId: String): TServiceItemRecord? {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this).where(
                ID.eq(itemId)
            ).fetchOne()
        }
    }

    fun getItemByCode(dslContext: DSLContext, itemCode: String): TServiceItemRecord? {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this).where(
                ITEM_CODE.eq(itemCode)
            ).fetchOne()
        }
    }

    fun getItemParent(dslContext: DSLContext): Result<TServiceItemRecord?> {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this).where(
                PARENT_ID.isNotNull
            ).fetch()
        }
    }

    fun getItemByIds(dslContext: DSLContext, itemIds: String): Result<TServiceItemRecord?> {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this).where(
                ID.`in`(itemIds)
            ).fetch()
        }
    }

    fun getAllServiceItem(dslContext: DSLContext): Result<TServiceItemRecord>? {
        return with(TServiceItem.T_SERVICE_ITEM) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }
}