package com.example.upload

import com.example.upload.domain.member.member.entity.Member
import com.example.upload.domain.member.member.service.MemberService
import com.example.upload.domain.post.comment.controller.ApiV1CommentController
import com.example.upload.domain.post.post.service.PostService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1CommentControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postService: PostService

    @Autowired
    private lateinit var memberService: MemberService

    private lateinit var loginedMember: Member
    private lateinit var token: String

    @BeforeEach
    fun login() {
        loginedMember = memberService.findByUsername("user1").get()
        token = memberService.getAuthToken(loginedMember)
    }

    @Test
    @DisplayName("댓글 작성")
    @Throws(Exception::class)
    fun write() {
        val postId: Long = 1
        val content = "댓글 내용"

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/posts/${postId}/comments")
                    .header("Authorization", "Bearer $token")
                    .content(
                        """
                                        {
                                            "content": "$content"
                                        }
                                        
                                        """
                            .trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        val post = postService.getItem(postId).get()
        val comment = post.latestComment

        resultActions
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1CommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("write"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${comment.id}번 댓글 작성이 완료되었습니다."))
    }

    @Test
    @DisplayName("댓글 수정")
    @Throws(Exception::class)
    fun modify() {
        val postId: Long = 1
        val commentId: Long = 1
        val content = "댓글 내용"

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/posts/${postId}/comments/${commentId}")
                    .header("Authorization", "Bearer $token")
                    .content(
                        """
                                        {
                                            "content": "%s"
                                        }
                                        
                                        """
                            .trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1CommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("modify"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${commentId}번 댓글 수정이 완료되었습니다."))
    }

    @Test
    @DisplayName("댓글 삭제")
    @Throws(Exception::class)
    fun delete1() {
        val postId: Long = 1
        val commentId: Long = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/posts/${postId}/comments/${commentId}")
                    .header("Authorization", "Bearer $token")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1CommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("delete"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${commentId}번 댓글 삭제가 완료되었습니다."))
    }

    @Test
    @DisplayName("댓글 다건 조회")
    @Throws(Exception::class)
    fun items() {
        val postId: Long = 1

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/posts/${postId}/comments"
                )
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1CommentController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getItems"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(2))
    }
}
