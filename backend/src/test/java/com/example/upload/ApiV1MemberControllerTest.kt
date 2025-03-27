package com.example.upload

import com.example.upload.domain.member.member.controller.ApiV1MemberController
import com.example.upload.domain.member.member.entity.Member
import com.example.upload.domain.member.member.service.MemberService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ApiV1MemberControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var memberService: MemberService

    private lateinit var loginedMember: Member
    private lateinit var token: String

    @BeforeEach
    fun login() {
        loginedMember = memberService.findByUsername("user1").get()
        token = memberService.getAuthToken(loginedMember)
    }


    @Throws(Exception::class)
    private fun checkMember(resultActions: ResultActions, member: Member) {
        resultActions
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(member.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.nickname").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.profileImgUrl").value(member.profileImgUrlOrDefaultUrl))
    }

    @Throws(Exception::class)
    private fun joinRequest(username: String, password: String, nickname: String): ResultActions {
        return mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/members/join")
                    .content(
                        """
                                        {
                                            "username": "$username",
                                            "password": "$password",
                                            "nickname": "$nickname"
                                        }
                                        
                                        """
                            .trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("회원 가입1")
    @Throws(Exception::class)
    fun join1() {
        val username = "userNew"
        val password = "1234"
        val nickname = "무명"

        val resultActions = joinRequest(username, password, nickname)
        val member = memberService.findByUsername("userNew").get()

        Assertions.assertThat(member.nickname).isEqualTo(nickname)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("회원 가입이 완료되었습니다."))

        checkMember(resultActions, member)
    }

    @Test
    @DisplayName("회원 가입2 - username이 이미 존재하는 케이스")
    @Throws(Exception::class)
    fun join2() {
        val username = "user1"
        val password = "1234"
        val nickname = "무명"

        val resultActions = joinRequest(username, password, nickname)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("409-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("이미 사용중인 아이디입니다."))
    }

    @Test
    @DisplayName("회원 가입3 - 입력 데이터 누락")
    @Throws(Exception::class)
    fun join3() {
        val username = ""
        val password = ""
        val nickname = ""

        val resultActions = joinRequest(username, password, nickname)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("join"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.msg").value(
                    """
                        nickname : NotBlank : must not be blank
                        password : NotBlank : must not be blank
                        username : NotBlank : must not be blank
                        
                        """.trimIndent().trim { it <= ' ' }
                )
            )
    }


    @Throws(Exception::class)
    private fun loingRequest(username: String, password: String): ResultActions {
        return mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/members/login")
                    .content(
                        """
                                        {
                                            "username": "$username",
                                            "password": "$password"
                                        }
                                        
                                        """
                            .trimIndent()
                    )
                    .contentType(
                        MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                    )
            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("로그인 - 성공")
    @Throws(Exception::class)
    fun login1() {
        val username = "user1"
        val password = "user11234"

        // 요청
        val resultActions = loingRequest(username, password)
        val member = memberService.findByUsername(username).get()

        // 응답. (요청 처리 결과)
        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("${member.nickname}님 환영합니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item.id").value(member.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.item.nickname").value(member.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiKey").value(member.apiKey))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").exists())
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.item.profileImgUrl").value(member.profileImgUrlOrDefaultUrl)
            )

        resultActions
            .andExpect { mvcResult: MvcResult ->
                val apiKey = mvcResult.response.getCookie("apiKey")
                Assertions.assertThat(apiKey).isNotNull()
                Assertions.assertThat(apiKey!!.name).isEqualTo("apiKey")
                Assertions.assertThat(apiKey.value).isNotBlank()
                Assertions.assertThat(apiKey.domain).isEqualTo("localhost")
                Assertions.assertThat(apiKey.path).isEqualTo("/")
                Assertions.assertThat(apiKey.isHttpOnly).isTrue()
                Assertions.assertThat(apiKey.secure).isTrue()

                val accessToken = mvcResult.response.getCookie("accessToken")

                Assertions.assertThat(accessToken).isNotNull()
                Assertions.assertThat(accessToken!!.name).isEqualTo("accessToken")
                Assertions.assertThat(accessToken.value).isNotBlank()
                Assertions.assertThat(accessToken.domain).isEqualTo("localhost")
                Assertions.assertThat(accessToken.path).isEqualTo("/")
                Assertions.assertThat(accessToken.isHttpOnly).isTrue()
                Assertions.assertThat(accessToken.secure).isTrue()
            }
    }

    @Test
    @DisplayName("로그인 - 실패 - 비밀번호 틀림")
    @Throws(Exception::class)
    fun login2() {
        val username = "user1"
        val password = "1234"

        val resultActions = loingRequest(username, password)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("로그인 - 실패 - 존재하지 않는 username")
    @Throws(Exception::class)
    fun login3() {
        val username = "aaaaa"
        val password = "1234"

        val resultActions = loingRequest(username, password)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("잘못된 아이디입니다."))
    }

    @Test
    @DisplayName("로그인 - 실패 - username 누락")
    @Throws(Exception::class)
    fun login4() {
        val username = ""
        val password = "123123"

        val resultActions = loingRequest(username, password)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("username : NotBlank : must not be blank"))
    }

    @Test
    @DisplayName("로그인 - 실패 - password 누락")
    @Throws(Exception::class)
    fun login5() {
        val username = "123123"
        val password = ""

        val resultActions = loingRequest(username, password)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("password : NotBlank : must not be blank"))
    }

    @Throws(Exception::class)
    private fun meRequest(apiKey: String): ResultActions {
        return mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/members/me")
                    .header("Authorization", "Bearer $apiKey")

            )
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    @DisplayName("내 정보 조회")
    @Throws(Exception::class)
    fun me1() {
        val apiKey = loginedMember.apiKey
        val token = memberService.getAuthToken(loginedMember)

        val resultActions = meRequest(token)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("내 정보 조회가 완료되었습니다."))

        checkMember(resultActions, loginedMember)
    }

    @Test
    @DisplayName("내 정보 조회 - 실패 - 잘못된 api key")
    @Throws(Exception::class)
    fun me2() {
        val apiKey = ""

        val resultActions = meRequest(apiKey)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("잘못된 인증키입니다."))
    }

    @Test
    @DisplayName("내 정보 조회 - 만료된 accessToken 사용")
    @Throws(Exception::class)
    fun me3() {
        val apiKey = loginedMember.apiKey
        val expiredToken =
            "$apiKey eyJhbGciOiJIUzUxMiJ9.eyJpZCI6MywidXNlcm5hbWUiOiJ1c2VyMSIsImlhdCI6MTczOTI0MDc0NiwiZXhwIjoxNzM5MjQwNzUxfQ.tm-lhZpkazdOtshyrdtq0ioJCampFzx8KBf-alfVS4JUp7zJJchYdYtjMfKtW7c3t4Fg5fEY12pPt6naJjhV-Q"

        val resultActions = meRequest(expiredToken)

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("me"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("내 정보 조회가 완료되었습니다."))

        checkMember(resultActions, loginedMember)
    }

    @Test
    @DisplayName("로그아웃")
    @Throws(Exception::class)
    fun logout() {
        val resultActions = mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/members/logout")
        )

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("logout"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("로그아웃 되었습니다."))


        resultActions.andExpect { mvcResult ->
            val apiKey = mvcResult.response.getCookie("apiKey")
            Assertions.assertThat(apiKey).isNotNull()
            Assertions.assertThat(apiKey!!.maxAge).isZero()

            val accessToken = mvcResult.response.getCookie("accessToken")
            Assertions.assertThat(accessToken).isNotNull()
            Assertions.assertThat(accessToken!!.maxAge).isZero()

        }
    }
}
