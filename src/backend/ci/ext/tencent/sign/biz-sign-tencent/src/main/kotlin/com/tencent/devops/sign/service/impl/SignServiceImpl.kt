package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.pojo.IpaCustomizedSignRequest
import com.tencent.devops.sign.service.SignService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class SignServiceImpl : SignService {

    @Value("\${sign.workspace:#{null}}")
    val workspace: String = ""

    fun resignIpaPackage(
        userId: String,
        ipaSignInfo: String?,
        inputStream: InputStream
    ): Result<String?> {
        logger.info("the upload file info is:$ipaSignInfo")
//        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
//        val index = fileName.lastIndexOf(".")
//        val fileSuffix = fileName.substring(index + 1)
//
//        if (!fileSuffix.contains("ipa") && !fileSuffix.contains("IPA")) {
//            throw InvalidParamException(
//                message = "该文件不是正确的IPA包",
//                params = arrayOf(fileName)
//            )
//        }
//
//        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
//        file.outputStream().use {
//            inputStream.copyTo(it)
//        }
//
//        file.copyTo(
//            target = File(workspace + File.separator + fileName),
//            overwrite = true
//        )

        return Result("")
    }

    fun resignCustomizedIpaPackage(
        userId: String,
        ipaCustomizedSignRequest: IpaCustomizedSignRequest,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        logger.info("the upload file info is:$disposition")
        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)

        if (!fileSuffix.contains("ipa") && !fileSuffix.contains("IPA")) {
            throw InvalidParamException(
                message = "该文件不是正确的IPA包",
                params = arrayOf(fileName)
            )
        }

        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }

        file.copyTo(
            target = File(workspace + File.separator + fileName),
            overwrite = true
        )

        return Result(fileName)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }

    override fun signIpaAndArchive(userId: String, ipaSignInfoHeader: String, ipaInputStream: InputStream): String? {
        TODO("Not yet implemented")
    }

    override fun resignIpaPackage(
        ipaPackage: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>?
    ): File {
        TODO("Not yet implemented")
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): Map<String, MobileProvisionInfo> {
        TODO("Not yet implemented")
    }

    override fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo {
        TODO("Not yet implemented")
    }
}