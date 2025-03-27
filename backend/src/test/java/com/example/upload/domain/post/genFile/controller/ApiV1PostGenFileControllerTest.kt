package com.example.upload.domain.post.genFile.controller

import com.example.upload.domain.member.member.service.MemberService
import com.example.upload.domain.post.genFile.entity.PostGenFile
import com.example.upload.domain.post.post.service.PostService
import com.example.upload.global.app.AppConfig.Companion.getTempDirPath
import com.example.upload.standard.util.SampleResource
import com.example.upload.standard.util.Ut.file.downloadByHttp
import com.example.upload.standard.util.Ut.file.rm
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.io.FileInputStream


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1PostGenFileControllerTest {
    @Autowired
    private lateinit var postService: PostService

    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var mvc: MockMvc

    @Test
    @DisplayName("다건 조회")
    @Throws(Exception::class)
    fun t1() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/posts/1/genFiles")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostGenFileController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("items"))
            .andExpect(MockMvcResultMatchers.status().isOk())

        val postGenFiles: List<PostGenFile> = postService
            .getItem(1).get().genFiles

        postGenFiles.forEachIndexed { i, file ->
            resultActions.andExpect {
                jsonPath("$[$i].id").value(file.id)
                jsonPath("$[$i].createDate").value(startsWith(file.createdDate.toString().substring(0, 20)))
                jsonPath("$[$i].modifyDate").value(startsWith(file.modifiedDate.toString().substring(0, 20)))
                jsonPath("$[$i].postId").value(file.post.id)
                jsonPath("$[$i].typeCode").value(file.typeCode.toString())
                jsonPath("$[$i].fileExtTypeCode").value(file.fileExtTypeCode)
                jsonPath("$[$i].fileExtType2Code").value(file.fileExtType2Code)
                jsonPath("$[$i].fileSize").value(file.fileSize)
                jsonPath("$[$i].fileNo").value(file.fileNo)
                jsonPath("$[$i].fileExt").value(file.fileExt)
                jsonPath("$[$i].fileDateDir").value(file.fileDateDir)
                jsonPath("$[$i].originalFileName").value(file.originalFileName)
                jsonPath("$[$i].downloadUrl").value(file.downloadUrl)
                jsonPath("$[$i].publicUrl").value(file.publicUrl)
                jsonPath("$[$i].fileName").value(file.fileName)
            }
        }
    }

    @Test
    @DisplayName("새 파일 등록")
    @WithUserDetails("user2")
    @Throws(
        Exception::class
    )
    fun t2() {
        val newFilePath = downloadByHttp("https://picsum.photos/id/237/200/300", getTempDirPath())

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/posts/9/genFiles/" + PostGenFile.TypeCode.attachment)
                    .file(
                        MockMultipartFile(
                            "files",
                            SampleResource.IMG_JPG_SAMPLE1.originalFileName,
                            SampleResource.IMG_JPG_SAMPLE1.contentType,
                            FileInputStream(newFilePath)
                        )
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.getItem(9).get()
        println(post.genFiles.size)

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostGenFileController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("makeNewItems"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("1개의 파일이 생성되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].createDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].modifyDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].postId").value(9))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].typeCode").value(PostGenFile.TypeCode.attachment.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExtTypeCode")
                    .value(SampleResource.IMG_JPG_SAMPLE1.fileExtTypeCode)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExtType2Code")
                    .value(SampleResource.IMG_JPG_SAMPLE1.fileExtType2Code)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileSize").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileNo").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExt").value(SampleResource.IMG_JPG_SAMPLE1.fileExt)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileDateDir").isString())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].originalFileName")
                    .value(SampleResource.IMG_JPG_SAMPLE1.originalFileName)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].downloadUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].publicUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileName").isString())

        rm(newFilePath)
    }

    @Test
    @DisplayName("새 파일 등록(다건)")
    @WithUserDetails("user2")
    @Throws(
        Exception::class
    )
    fun t4() {
        val newFilePath1 = SampleResource.IMG_JPG_SAMPLE1.makeCopy()
        val newFilePath2 = SampleResource.IMG_JPG_SAMPLE2.makeCopy()

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/v1/posts/9/genFiles/" + PostGenFile.TypeCode.attachment)
                    .file(
                        MockMultipartFile(
                            "files",
                            SampleResource.IMG_JPG_SAMPLE1.originalFileName,
                            SampleResource.IMG_JPG_SAMPLE1.contentType,
                            FileInputStream(newFilePath1)
                        )
                    )
                    .file(
                        MockMultipartFile(
                            "files",
                            SampleResource.IMG_JPG_SAMPLE2.originalFileName,
                            SampleResource.IMG_JPG_SAMPLE2.contentType,
                            FileInputStream(newFilePath2)
                        )
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1PostGenFileController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("makeNewItems"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("2개의 파일이 생성되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].createDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].modifyDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].postId").value(9))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].typeCode").value(PostGenFile.TypeCode.attachment.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExtTypeCode")
                    .value(SampleResource.IMG_JPG_SAMPLE1.fileExtTypeCode)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExtType2Code")
                    .value(SampleResource.IMG_JPG_SAMPLE1.fileExtType2Code)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileSize").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileNo").value(1))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].fileExt").value(SampleResource.IMG_JPG_SAMPLE1.fileExt)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileDateDir").isString())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[0].originalFileName")
                    .value(SampleResource.IMG_JPG_SAMPLE1.originalFileName)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].downloadUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].publicUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].fileName").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].createDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].modifyDate").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].postId").value(9))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].typeCode").value(PostGenFile.TypeCode.attachment.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[1].fileExtTypeCode")
                    .value(SampleResource.IMG_JPG_SAMPLE2.fileExtTypeCode)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[1].fileExtType2Code")
                    .value(SampleResource.IMG_JPG_SAMPLE2.fileExtType2Code)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].fileSize").isNumber())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].fileNo").value(2))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[1].fileExt").value(SampleResource.IMG_JPG_SAMPLE2.fileExt)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].fileDateDir").isString())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data[1].originalFileName")
                    .value(SampleResource.IMG_JPG_SAMPLE2.originalFileName)
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].downloadUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].publicUrl").isString())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].fileName").isString())

        rm(newFilePath1)
        rm(newFilePath2)
    }
}