package com.tencent.devops.dockerhost.services

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import org.slf4j.LoggerFactory

abstract class AbstractDockerHostBuildService constructor(
    dockerHostConfig: DockerHostConfig,
    private val dockerHostBuildApi: DockerHostBuildResourceApi
){

    private val logger = LoggerFactory.getLogger(AbstractDockerHostBuildService::class.java)

    private val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerConfig(dockerHostConfig.dockerConfig)
        .withApiVersion(dockerHostConfig.apiVersion)
        .build()

    var httpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(30000)
        .build()

    var longHttpClient: DockerHttpClient = OkDockerHttpClient.Builder()
        .dockerHost(config.dockerHost)
        .sslConfig(config.sslConfig)
        .connectTimeout(5000)
        .readTimeout(300000)
        .build()

    val httpDockerCli: DockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(httpClient).build()

    val httpLongDockerCli: DockerClient = DockerClientBuilder.getInstance(config).withDockerHttpClient(longHttpClient).build()

    abstract fun createContainer(dockerHostBuildInfo: DockerHostBuildInfo): String

    abstract fun stopContainer(buildId: String, containerId: String, vmSeqId: Int)

    fun log(buildId: String, message: String, tag: String?, containerHashId: String?) {
        return log(buildId, false, message, tag, containerHashId)
    }

    fun log(buildId: String, red: Boolean, message: String, tag: String?, containerHashId: String?) {
        logger.info("write log to dispatch, buildId: $buildId, message: $message")
        try {
            dockerHostBuildApi.postLog(
                buildId = buildId,
                red = red,
                message = message,
                tag = tag,
                jobId = containerHashId
            )
        } catch (t: Throwable) {
            logger.info("write log to dispatch failed")
        }
    }
}