/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.mongodb.BasicDBObject;
import com.tencent.bk.codecc.task.model.ToolCheckerSetEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码检查任务持久层代码
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Repository
public class ToolDao
{
    @Autowired
    private MongoTemplate mongoTemplate;


    public void updatePathByTaskIdAndToolName(long taskId, String toolName, List<String> pathList)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).
                addCriteria(Criteria.where("tool_name").is(toolName));
        Update update = new Update();
        update.set("default_filter_path", pathList);
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void updateToolStepStatusByTaskIdAndToolName(ToolConfigBaseVO toolConfigBaseVO)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(toolConfigBaseVO.getTaskId())).
                addCriteria(Criteria.where("tool_name").is(toolConfigBaseVO.getToolName()));
        Update update = new Update();
        update.set("cur_step", toolConfigBaseVO.getCurStep());
        update.set("step_status", toolConfigBaseVO.getStepStatus());
        update.set("current_build_id", toolConfigBaseVO.getCurrentBuildId());
        update.set("updatedBy", ComConstants.SYSTEM_USER);
        update.set("updatedDate", System.currentTimeMillis());
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void updateParamJson(ToolConfigInfoEntity toolConfigInfoEntity, String userName)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(toolConfigInfoEntity.getTaskId())).
                addCriteria(Criteria.where("tool_name").is(toolConfigInfoEntity.getToolName()));
        Update update = new Update();
        update.set("param_json", toolConfigInfoEntity.getParamJson());
        update.set("updated_by", userName);
        update.set("updated_date", System.currentTimeMillis());
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void clearCheckerSet(long taskId, List<String> toolNames)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).
                addCriteria(Criteria.where("tool_name").in(toolNames));
        Update update = new Update();
        update.set("checker_set", null);
        mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class);
    }

    public void setCheckerSet(long taskId, List<ToolCheckerSetEntity> toolCheckerSets)
    {
        if (CollectionUtils.isNotEmpty(toolCheckerSets))
        {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ToolConfigInfoEntity.class);
            for (ToolCheckerSetEntity toolCheckerSetVO : toolCheckerSets)
            {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)).
                        addCriteria(Criteria.where("tool_name").is(toolCheckerSetVO.getToolName()));
                Update update = new Update();
                update.set("checker_set", toolCheckerSetVO);
                ops.updateOne(query, update);
            }
            ops.execute();
        }
    }


    /**
     * 获取工具配置信息(platform)
     *
     * @param toolName   工具名
     * @param platformIp Platform IP
     * @param taskIds    任务ID集合
     * @param pageable   分页器
     * @return list
     */
    public List<ToolConfigInfoEntity> queryToolPlatformInfo(String toolName, String platformIp, Long taskIds,
            Pageable pageable)
    {
        BasicDBObject fieldsObj = new BasicDBObject();
        fieldsObj.put("task_id", true);
        fieldsObj.put("tool_name", true);
        fieldsObj.put("platform_ip", true);
        Query query = new BasicQuery(new BasicDBObject(), fieldsObj);

        if (StringUtils.isNotEmpty(toolName))
        {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }

        if (StringUtils.isNotEmpty(platformIp))
        {
            query.addCriteria(Criteria.where("platform_ip").is(platformIp));
        }

        if (taskIds != null && taskIds != 0)
        {
            query.addCriteria(Criteria.where("task_id").is(taskIds));
        }

        if (pageable != null)
        {
            query.with(pageable);
        }
        return mongoTemplate.find(query, ToolConfigInfoEntity.class);
    }

    /**
     * 更新工具配置
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @param specConfig 特殊配置
     * @param userName   更改人
     */
    public Boolean updateToolConfigInfo(Long taskId, String toolName, String userName, String specConfig,
            String platformIp)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)).addCriteria(Criteria.where("tool_name").is(toolName));

        Update update = new Update();
        if (specConfig != null)
        {
            update.set("spec_config", specConfig);
        }

        if (StringUtils.isNotBlank(platformIp))
        {
            update.set("platform_ip", platformIp);
        }

        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        return mongoTemplate.updateMulti(query, update, ToolConfigInfoEntity.class).isUpdateOfExisting();
    }
}
