package com.example.changtest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MainController 单元测试")
public class MainControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MainController controller = new MainController();

        View mockView = mock(View.class);
        View redirectView = mock(View.class);
        ViewResolver viewResolver = (viewName, locale) ->
                viewName.startsWith("redirect:") ? redirectView : mockView;

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    // ======================== GET /main 测试 ========================

    @Nested
    @DisplayName("主页")
    class MainPageTests {

        @Test
        @DisplayName("GET /main - 有session时显示用户名")
        void testMainPageWithSession() throws Exception {
            MockHttpSession session = new MockHttpSession();
            // 新方案: 仅存储 username 字符串，不存储整个 User 实体
            session.setAttribute("username", "testuser");

            mockMvc.perform(get("/main").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attribute("username", "testuser"));
        }

        @Test
        @DisplayName("GET /main - 无session时也能渲染")
        void testMainPageWithoutSession() throws Exception {
            mockMvc.perform(get("/main"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attributeDoesNotExist("username"));
        }

        @Test
        @DisplayName("GET /main - session中username为null")
        void testMainPageWithNullUser() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("username", null);

            mockMvc.perform(get("/main").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attributeDoesNotExist("username"));
        }
    }

    // ======================== GET / 测试 ========================

    @Nested
    @DisplayName("根路径")
    class RootPathTests {

        @Test
        @DisplayName("GET / - 重定向到 /main")
        void testRootRedirectsToMain() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(view().name("redirect:/main"));
        }
    }
}
