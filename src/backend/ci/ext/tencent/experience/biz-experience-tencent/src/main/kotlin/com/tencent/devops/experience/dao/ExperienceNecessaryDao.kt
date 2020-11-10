package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceNecessary
import com.tencent.devops.model.experience.tables.records.TExperienceNecessaryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ExperienceNecessaryDao {
    fun list(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        platform: String?
    ): Result<TExperienceNecessaryRecord> {
        return with(TExperienceNecessary.T_EXPERIENCE_NECESSARY) {
            dslContext.selectFrom(this)
                .where(ONLINE.eq(true))
                .orderBy(UPDATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun count(dslContext: DSLContext, platform: String?): Int {
        return with(TExperienceNecessary.T_EXPERIENCE_NECESSARY) {
            dslContext.selectCount().from(this)
                .where(ONLINE.eq(true))
                .fetchOne().value1()
        }
    }
}