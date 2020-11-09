package com.tencent.devops.dispatch.docker.pojo.enums

enum class DockerHostClusterType {
    /**
     * 公共构建机集群
     */
    COMMON,

    /**
     * 无编译环境构建机群
     */
    AGENT_LESS
}