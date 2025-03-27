package com.example.upload.standard.util

import com.example.upload.global.app.AppConfig
import com.example.upload.standard.util.Ut.file.delete
import com.example.upload.standard.util.Ut.file.downloadByHttp
import com.example.upload.standard.util.Ut.file.getExtensionByTika
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class FileUtTest {
    @Test
    @DisplayName("downloadByHtztp")
    fun t1() {
        val newFilePath = downloadByHttp("https://picsum.photos/id/237/200/300", AppConfig.getTempDirPath())

        // newFilePath 의 확장자가 jpg 인지 확인
        Assertions.assertThat(newFilePath).endsWith(".jpg")

        delete(newFilePath)
    }

    @Test
    @DisplayName("getExtensionByTika")
    fun tika() {
        val newFilePath = downloadByHttp("https://picsum.photos/id/237/200/300", AppConfig.getTempDirPath())

        val ext = getExtensionByTika(newFilePath)
        Assertions.assertThat(ext).isEqualTo("jpg")

        delete(newFilePath)
    }
}