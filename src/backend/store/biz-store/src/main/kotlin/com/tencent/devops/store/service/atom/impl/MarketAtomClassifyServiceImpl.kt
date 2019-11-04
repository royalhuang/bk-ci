/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomClassifyDao
import com.tencent.devops.store.pojo.atom.MarketAtomClassify
import com.tencent.devops.store.service.atom.MarketAtomClassifyService
import com.tencent.devops.store.service.common.AbstractClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 插件市场-插件分类业务逻辑类
 *
 * since: 2019-01-15
 */
@Service("ATOM_CLASSIFY_SERVICE")
class MarketAtomClassifyServiceImpl @Autowired constructor() : MarketAtomClassifyService, AbstractClassifyService() {

    private val logger = LoggerFactory.getLogger(MarketAtomClassifyServiceImpl::class.java)

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var atomDao: AtomDao

    @Autowired
    lateinit var marketAtomClassifyDao: MarketAtomClassifyDao

    /**
     * 获取所有插件分类信息
     */
    override fun getAllAtomClassify(): Result<List<MarketAtomClassify>> {
        val marketAtomClassifyList = mutableListOf<MarketAtomClassify>()
        val marketAtomClassifyRecords = marketAtomClassifyDao.getAllAtomClassify(dslContext)
        marketAtomClassifyRecords?.forEach {
            val id = it["id"] as String
            val classifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
            val atomNum = it["atomNum"] as? Int
            val createTime = it["createTime"] as LocalDateTime
            val updateTime = it["updateTime"] as LocalDateTime
            marketAtomClassifyList.add(
                MarketAtomClassify(
                    id,
                    classifyCode,
                    classifyName,
                    atomNum ?: 0,
                    createTime.timestampmilli(),
                    updateTime.timestampmilli()
                )
            )
        }
        logger.info("the marketAtomClassifyList is:$marketAtomClassifyList")
        return Result(marketAtomClassifyList)
    }

    override fun getDeleteClassifyFlag(classifyId: String): Boolean {
        // 允许删除分类是条件：1、该分类下的原子插件都不处于上架状态 2、该分类下的原子插件如果处于下架中或者已下架状态但已经没人在用
        var flag = false
        val releaseAtomNum = atomDao.countReleaseAtomNumByClassifyId(dslContext, classifyId)
        logger.info("the releaseAtomNum is :$releaseAtomNum")
        if (releaseAtomNum == 0) {
            val undercarriageAtomNum = atomDao.countUndercarriageAtomNumByClassifyId(dslContext, classifyId)
            logger.info("the undercarriageAtomNum is :$undercarriageAtomNum")
            if (undercarriageAtomNum == 0) {
                flag = true
            }
        }
        return flag
    }
}
