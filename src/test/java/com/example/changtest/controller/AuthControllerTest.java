package com.example.changtest.controller;

import com.example.changtest.entity.User;
import com.example.changtest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController 单元测试")
public class AuthControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        AuthController controller = new AuthController(userService);

        View mockView = mock(View.class);
        View redirectView = mock(View.class);
        ViewResolver viewResolver = (viewName, locale) ->
                viewName.startsWith("redirect:") ? redirectView : mockView;

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    // ======================== GET /login 测试 ========================

    @Nested
    @DisplayName("登录页面")
    class LoginPageTests {

        @Test
        @DisplayName("GET /login - 返回登录页面")
        void testLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }
    }

    // ======================== POST /login 测试 ========================

    @Nested
    @DisplayName("登录提交")
    class LoginSubmitTests {

        @Test
        @DisplayName("POST /login - 正确凭据应重定向到 /main")
        void testLoginSuccess() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encoded");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct"))
                    .andExpect(view().name("redirect:/main"))
                    .andExpect(request().sessionAttribute("user", user));
        }

        @Test
        @DisplayName("POST /login - 错误密码返回登录页")
        void testLoginFailure() throws Exception {
            when(userService.authenticate("testuser", "wrong")).thenReturn(null);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "wrong"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"))
                    .andExpect(model().attribute("loginError", "用户名或密码错误"))
                    .andExpect(model().attribute("username", "testuser"));
        }

        @Test
        @DisplayName("POST /login - 不存在用户返回登录页")
        void testLoginUserNotFound() throws Exception {
            when(userService.authenticate("ghost", "whatever")).thenReturn(null);

            mockMvc.perform(post("/login")
                            .param("username", "ghost")
                            .param("password", "whatever"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"));
        }
    }

    // ======================== GET /register 测试 ========================

    @Nested
    @DisplayName("注册页面")
    class RegisterPageTests {

        @Test
        @DisplayName("GET /register - 返回注册页面")
        void testRegisterPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"));
        }
    }

    // ======================== POST /register 测试 ========================

    @Nested
    @DisplayName("注册提交")
    class RegisterSubmitTests {

        @Test
        @DisplayName("POST /register - 注册成功重定向到登录页")
        void testRegisterSuccess() throws Exception {
            doNothing().when(userService).register("newuser", "password123");

            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "password123")
                            .param("confirmPassword", "password123"))
                    .andExpect(view().name("redirect:/login?registered"));
        }

        @Test
        @DisplayName("POST /register - 两次密码不一致")
        void testRegisterPasswordMismatch() throws Exception {
            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "password123")
                            .param("confirmPassword", "different"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("registerError"))
                    .andExpect(model().attribute("registerError", "两次输入的密码不一致"))
                    .andExpect(model().attribute("username", "newuser"));

            verify(userService, never()).register(anyString(), anyString());
        }

        @Test
        @DisplayName("POST /register - 用户名已存在")
        void testRegisterDuplicateUsername() throws Exception {
            doThrow(new IllegalArgumentException("用户名已存在"))
                    .when(userService).register("existing", "password123");

            mockMvc.perform(post("/register")
                            .param("username", "existing")
                            .param("password", "password123")
                            .param("confirmPassword", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("registerError"))
                    .andExpect(model().attribute("registerError", "用户名已存在"))
                    .andExpect(model().attribute("username", "existing"));
        }

        @Test
        @DisplayName("POST /register - 用户名为空")
        void testRegisterEmptyUsername() throws Exception {
            doThrow(new IllegalArgumentException("用户名不能为空"))
                    .when(userService).register("", "password123");

            mockMvc.perform(post("/register")
                            .param("username", "")
                            .param("password", "password123")
                            .param("confirmPassword", "password123"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attribute("registerError", "用户名不能为空"));
        }

        @Test
        @DisplayName("POST /register - 密码太短")
        void testRegisterPasswordTooShort() throws Exception {
            doThrow(new IllegalArgumentException("密码长度不能少于6个字符"))
                    .when(userService).register("newuser", "12345");

            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "12345")
                            .param("confirmPassword", "12345"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attribute("registerError", "密码长度不能少于6个字符"));
        }
    }

    // ======================== GET /logout 测试 ========================

    @Nested
    @DisplayName("登出")
    class LogoutTests {

        @Test
        @DisplayName("GET /logout - 重定向到登录页")
        void testLogout() throws Exception {
            mockMvc.perform(get("/logout"))
                    .andExpect(view().name("redirect:/login?logout"));
        }
    }
}
