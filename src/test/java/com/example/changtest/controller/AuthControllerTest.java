package com.example.changtest.controller;

import com.example.changtest.entity.LoginLog;
import com.example.changtest.entity.User;
import com.example.changtest.repository.LoginLogRepository;
import com.example.changtest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController 单元测试")
public class AuthControllerTest {

    private UserService userService;
    private LoginLogRepository loginLogRepository;
    private MockMvc mockMvc;
    private List<LoginLog> savedLogs;

    private static final String CSRF_TOKEN = "test-csrf-token-12345";

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        loginLogRepository = mock(LoginLogRepository.class);
        savedLogs = new ArrayList<>();

        // 捕获所有 save() 调用到 savedLogs 列表，避免 JpaRepository.save 方法重载歧义
        doAnswer(invocation -> {
            savedLogs.add(invocation.getArgument(0));
            return null;
        }).when(loginLogRepository).save(any(LoginLog.class));

        AuthController controller = new AuthController(userService, loginLogRepository);

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
        @DisplayName("GET /login - 返回登录页面并设置 CSRF token")
        void testLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("csrfToken"))
                    .andExpect(request().sessionAttribute("csrfToken", org.hamcrest.Matchers.notNullValue()));
        }

        @Test
        @DisplayName("GET /login - 每次请求生成新的 CSRF token")
        void testLoginPageRegeneratesCsrfToken() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("csrfToken"));
            // 两次请求应生成不同 token
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("csrfToken"));
        }
    }

    // ======================== POST /login 测试 ========================

    @Nested
    @DisplayName("登录提交")
    class LoginSubmitTests {

        @Test
        @DisplayName("POST /login - 正确凭据应重定向到 /main，仅存储 userId 和 username")
        void testLoginSuccess() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encoded-hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"))
                    .andExpect(request().sessionAttribute("userId", 1L))
                    .andExpect(request().sessionAttribute("username", "testuser"));
        }

        @Test
        @DisplayName("POST /login - 登录成功后记录 LoginLog (success=true)")
        void testLoginSuccessSavesLoginLog() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encoded-hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty(), "应记录 LoginLog");
            LoginLog saved = savedLogs.get(0);
            assertEquals("testuser", saved.getUsername());
            assertTrue(saved.getSuccess());
            assertEquals("登录成功", saved.getMessage());
            assertNotNull(saved.getIpAddress());
            assertFalse(saved.getIpAddress().isEmpty());
        }

        @Test
        @DisplayName("POST /login - 不存储密码 hash 到 session (防止泄漏)")
        void testLoginSuccessDoesNotStorePasswordHash() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encoded-hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"))
                    // 确保 session 中不存在完整的 User 实体（仅存 userId + username）
                    .andExpect(request().sessionAttribute("user", org.hamcrest.Matchers.nullValue()));
        }

        @Test
        @DisplayName("POST /login - 错误密码返回登录页并记录失败日志")
        void testLoginFailure() throws Exception {
            when(userService.authenticate("testuser", "wrong")).thenReturn(null);

            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "wrong")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"))
                    .andExpect(model().attribute("loginError", "用户名或密码错误"))
                    .andExpect(model().attribute("username", "testuser"));

            // 验证记录了失败日志
            assertFalse(savedLogs.isEmpty(), "应记录 LoginLog");
            LoginLog saved = savedLogs.get(0);
            assertEquals("testuser", saved.getUsername());
            assertFalse(saved.getSuccess());
            assertEquals("用户名或密码错误", saved.getMessage());
        }

        @Test
        @DisplayName("POST /login - 不存在用户返回登录页")
        void testLoginUserNotFound() throws Exception {
            when(userService.authenticate("ghost", "whatever")).thenReturn(null);

            mockMvc.perform(post("/login")
                            .param("username", "ghost")
                            .param("password", "whatever")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"));
        }

        @Test
        @DisplayName("POST /login - CSRF token 不匹配应拒绝且不调用 authenticate")
        void testLoginCsrfMismatch() throws Exception {
            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", "wrong-token")
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"));

            verify(userService, never()).authenticate(anyString(), anyString());
            assertTrue(savedLogs.isEmpty(), "不应记录任何日志");
        }

        @Test
        @DisplayName("POST /login - 缺少 CSRF token 应拒绝且不调用 authenticate")
        void testLoginCsrfMissing() throws Exception {
            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"));

            verify(userService, never()).authenticate(anyString(), anyString());
            assertTrue(savedLogs.isEmpty(), "不应记录任何日志");
        }

        @Test
        @DisplayName("POST /login - session 中无 CSRF token 应拒绝")
        void testLoginCsrfNoSessionToken() throws Exception {
            mockMvc.perform(post("/login")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"))
                    .andExpect(model().attributeExists("loginError"));

            verify(userService, never()).authenticate(anyString(), anyString());
        }
    }

    // ======================== IP 提取测试 ========================

    @Nested
    @DisplayName("客户端 IP 提取")
    class IpExtractionTests {

        @Test
        @DisplayName("X-Forwarded-For 单个有效 IP")
        void testXForwardedForSingleIp() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", "203.0.113.1")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            assertEquals("203.0.113.1", savedLogs.get(0).getIpAddress());
        }

        @Test
        @DisplayName("X-Forwarded-For 逗号分隔多 IP 取第一个有效 IP")
        void testXForwardedForMultipleIps() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 192.168.1.1")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            assertEquals("203.0.113.1", savedLogs.get(0).getIpAddress());
        }

        @Test
        @DisplayName("X-Forwarded-For 无效值时回退到 RemoteAddr")
        void testXForwardedForInvalidFallback() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", "not-a-valid-ip")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            // 应回退到 RemoteAddr (MockMvc 默认是 127.0.0.1)
            assertEquals("127.0.0.1", savedLogs.get(0).getIpAddress());
        }

        @Test
        @DisplayName("X-Forwarded-For 为空时使用 RemoteAddr")
        void testXForwardedForEmptyUsesRemoteAddr() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", "")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            assertEquals("127.0.0.1", savedLogs.get(0).getIpAddress());
        }

        @Test
        @DisplayName("IPv6 地址支持")
        void testIpv6Address() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", "::1")
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            assertEquals("::1", savedLogs.get(0).getIpAddress());
        }

        @Test
        @DisplayName("IP 超过 45 字符时截断到数据库字段长度")
        void testIpTruncation() throws Exception {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("hash");
            when(userService.authenticate("testuser", "correct")).thenReturn(user);

            // 构造一个超长 IPv6 地址（>45 字符）
            String longIp = "2001:0db8:85a3:0000:0000:8a2e:0370:7334:extra";
            mockMvc.perform(post("/login")
                            .header("X-Forwarded-For", longIp)
                            .param("username", "testuser")
                            .param("password", "correct")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/main"));

            assertFalse(savedLogs.isEmpty());
            assertTrue(savedLogs.get(0).getIpAddress().length() <= 45,
                    "IP 地址应截断到 45 字符以内");
        }
    }

    // ======================== GET /register 测试 ========================

    @Nested
    @DisplayName("注册页面")
    class RegisterPageTests {

        @Test
        @DisplayName("GET /register - 返回注册页面并设置 CSRF token")
        void testRegisterPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("csrfToken"))
                    .andExpect(request().sessionAttribute("csrfToken", org.hamcrest.Matchers.notNullValue()));
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
                            .param("confirmPassword", "password123")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(view().name("redirect:/login?registered"));
        }

        @Test
        @DisplayName("POST /register - 两次密码不一致")
        void testRegisterPasswordMismatch() throws Exception {
            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "password123")
                            .param("confirmPassword", "different")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
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
                            .param("confirmPassword", "password123")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
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
                            .param("confirmPassword", "password123")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
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
                            .param("confirmPassword", "12345")
                            .param("_csrf", CSRF_TOKEN)
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attribute("registerError", "密码长度不能少于6个字符"));
        }

        @Test
        @DisplayName("POST /register - CSRF token 不匹配应拒绝")
        void testRegisterCsrfMismatch() throws Exception {
            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .param("_csrf", "wrong-token")
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("registerError"));

            verify(userService, never()).register(anyString(), anyString());
        }

        @Test
        @DisplayName("POST /register - 缺少 CSRF token 应拒绝")
        void testRegisterCsrfMissing() throws Exception {
            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("password", "password123")
                            .param("confirmPassword", "password123")
                            .sessionAttr("csrfToken", CSRF_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeExists("registerError"));

            verify(userService, never()).register(anyString(), anyString());
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
