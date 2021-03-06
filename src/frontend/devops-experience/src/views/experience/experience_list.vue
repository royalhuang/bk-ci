<template>
    <div class="release-list-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <content-header>
            <div slot="left">{{ $route.meta.title }}</div>
            <div slot="right" v-if="showContent">
                <bk-checkbox :checked="getIsShowExpired" @change="toggleExpired">
                    显示已过期体验
                </bk-checkbox>
                <!-- <label class="bk-form-checkbox" style="margin-right: 0;">
                    <input type="checkbox"  @click="toggleExpired">
                </label> -->
                
            </div>
        </content-header>
        <section class="sub-view-port">
            <div v-if="showContent && releaseList.length">
                <bk-table
                    :data="releaseList"
                    style="cursor: pointer"
                    @row-click="toRowDetail"
                >
                    <bk-table-column label="文件名（版本号）" prop="name" min-width="150">
                        <template slot-scope="props">
                            <i v-if="props.row.expired" class="bk-icon icon-expired-experience"></i>
                            <span class="link-text" :title="`${props.row.name}（ ${props.row.version} ）`">{{ props.row.name }}（{{ props.row.version }}）</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="版本描述" prop="remark" min-width="100"></bk-table-column>
                    <bk-table-column label="平台" prop="platformLabel"></bk-table-column>
                    <bk-table-column label="来源" prop="sourceLabel"></bk-table-column>
                    <bk-table-column label="发布人" prop="creator"></bk-table-column>
                    <bk-table-column label="体验结束时间" prop="formatExpireDate"></bk-table-column>
                    <bk-table-column label="操作" width="150">
                        <template slot-scope="props">
                            <div class="operate-cell">
                                <template v-if="!props.row.expired && props.row.online">
                                    <span v-if="!props.row.permissions.canExperience " v-bk-tooltips="{ content: '你没有该版本的体验权限' }" class="bk-icon icon-qrcode"></span>
                                    <bk-popover placement="left" theme="light" v-if="props.row.permissions.canExperience">
                                        <i class="bk-icon icon-qrcode" @mouseover="requestUrl(props.row)"></i>
                                        <p slot="content" class="qrcode-box" v-if="props.row.permissions.canExperience" v-bkloading="{ isLoading: !curIndexItemUrl }">
                                            <qrcode class="qrcode-view" :text="curIndexItemUrl" :size="100"></qrcode>
                                        </p>
                                    </bk-popover>
                                </template>
                                <i v-bk-tooltips="{ content: '该体验已过期' }" class="bk-icon icon-qrcode expired-text" v-else></i>
                                <span class="edit" @click.stop="toEditRow(props.row)">编辑</span>
                                <span class="drop-off" @click.stop="toDropOff(props.row)" v-if="props.row.online && !props.row.expired">下架</span>
                                <span v-bk-tooltips="{ content: '该体验已下架' }" class="expired-text" v-else>下架</span>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
            <empty-data v-if="showContent && !releaseList.length" :empty-info="emptyInfo" :to-create-fn="toCreateFn">
            </empty-data>
        </section>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { convertTime } from '@/utils/util'
    import qrcode from '@/components/devops/qrcode'
    import emptyData from './empty-data'

    export default {
        components: {
            emptyData,
            qrcode
        },
        data () {
            return {
                showContent: false,
                curIndexItemUrl: '',
                defaultCover: require('@/images/qrcode_app.png'),
                releaseList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: '暂无体验',
                    desc: '您可以在新增体验中新增一个体验任务'
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ...mapGetters('experience', [
                'getIsShowExpired'
            ])
        },
        watch: {
            projectId () {
                this.init()
            },
            getIsShowExpired () {
                this.requestList()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取发布列表
             */
            async requestList () {
                try {
                    console.log(typeof this.getIsShowExpired)
                    const res = await this.$store.dispatch('experience/requestExpList', {
                        projectId: this.projectId,
                        params: {
                            expired: this.getIsShowExpired
                        }
                    })
                    const platformLabelMap = {
                        ANDROID: 'Android',
                        IOS: 'iOS'
                    }
                    const sourceLabelMap = {
                        PIPELINE: '流水线',
                        WEB: '手动创建'
                    }
                    this.releaseList = res.map(item => ({
                        ...item,
                        platformLabel: platformLabelMap[item.platform],
                        sourceLabel: sourceLabelMap[item.source],
                        formatExpireDate: this.localConvertTime(item.expireDate).split(' ')[0]
                    }))
                    console.log(this.releaseList)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }

                this.showContent = true
            },
            async requestUrl (row) {
                this.curIndexItemUrl = ''
                if (row.permissions.canExperience) {
                    try {
                        const res = await this.$store.dispatch('experience/requestExternalUrl', {
                            projectId: this.projectId,
                            experienceHashId: row.experienceHashId
                        })

                        this.curIndexItemUrl = res.url
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
            },
            toRowDetail (row) {
                this.$router.push({
                    name: 'experienceDetail',
                    params: {
                        projectId: this.projectId,
                        experienceId: row.experienceHashId,
                        type: 'detail'
                    }
                })
            },
            toEditRow (row) {
                if (row.permissions.canEdit) {
                    this.$router.push({
                        name: 'editExperience',
                        params: {
                            projectId: this.projectId,
                            experienceId: row.experienceHashId
                        }
                    })
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: '版本体验', option: '编辑' }
                        ],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=experience&role_manager=task:${row.experienceHashId}`
                    }

                    this.$showAskPermissionDialog(params)
                }
            },
            async toDropOff (row) {
                if (row.permissions.canEdit) {
                    this.$bkInfo({
                        title: `确认`,
                        subTitle: '确认下架该体验',
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/toOfflineExp', {
                                    projectId: this.projectId,
                                    experienceHashId: row.experienceHashId
                                })

                                message = '下架成功'
                                theme = 'success'
                            } catch (err) {
                                message = err.data ? err.data.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })

                                this.requestList()
                            }
                        }
                    })
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: '版本体验', option: '编辑' }
                        ],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=experience&role_manager=task:${row.experienceHashId}`
                    }

                    this.$showAskPermissionDialog(params)
                }
            },
            toggleExpired (isExpired) {
                this.$store.dispatch('experience/updateIsExpired', {
                    isExpired
                })
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            },
            toCreateFn () {
                this.$router.push({
                    name: 'createExperience',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../scss/conf';

    .release-list-wrapper {
        height: 100%;
        overflow: hidden;
        .link-text {
            color: $primaryColor;
        }
        .bk-icon.icon-expired-experience{
            position: absolute;
            left: -3px;
            top: -3px;
            font-size: 36px;
            color: $fontLigtherColor;
        }
        .operate-cell {
            display: flex;
            align-items: center;
            color: $primaryColor;
            .bk-tooltip {
                height: 12px;
            }
            > span {
                margin-left: 12px;
                &:first-child {
                    margin-left: 0;
                }
            }
            .expired-text {
                cursor: default;
                color: $fontLigtherColor;
            }
        }
    }
</style>
