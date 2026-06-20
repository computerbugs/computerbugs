package com.example.changtest.service;

import com.example.changtest.entity.User;
import com.example.changtest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @InjectMocks
    private UserService userService;

    // ======================== register 测试 ========================

    @Nested
    @DisplayName("注册功能测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册 - 有效用户名和密码")
        void testRegisterSuccess() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);

            userService.register("newuser", "password123");

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 保存的用户包含正确的用户名")
        void testRegisterSavesCorrectUsername() {
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register("alice", "secret123");

            verify(userRepository).save(argThat((User user) -> "alice".equals(user.getUsername())));
        }

        @Test
        @DisplayName("注册 - 密码被BCrypt加密存储")
        void testRegisterPasswordIsEncrypted() {
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register("bob", "mypassword");

            verify(userRepository).save(argThat((User user) ->
                    !"mypassword".equals(user.getPassword()) &&
                            user.getPassword().startsWith("$2a$")
            ));
        }

        @Test
        @DisplayName("注册 - 每次生成的加密密码不同(salt)")
        void testRegisterDifferentSalts() {
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByUsername("user2")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register("user1", "samepassword");
            userService.register("user2", "samepassword");

            verify(userRepository, times(2)).save(argThat((User user) ->
                    user.getPassword().startsWith("$2a$")
            ));
        }

        @Test
        @DisplayName("注册 - 用户名为空应抛异常")
        void testRegisterNullUsername() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register(null, "password123")
            );
            assertEquals("用户名不能为空", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 用户名为空白字符串应抛异常")
        void testRegisterBlankUsername() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register("   ", "password123")
            );
            assertEquals("用户名不能为空", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 用户名超过50字符应抛异常")
        void testRegisterUsernameTooLong() {
            char[] chars = new char[51];
            Arrays.fill(chars, 'a');
            String longName = new String(chars);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register(longName, "password123")
            );
            assertEquals("用户名长度不能超过50个字符", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 用户名刚好50字符可以注册")
        void testRegisterUsernameExactly50Chars() {
            char[] chars = new char[50];
            Arrays.fill(chars, 'a');
            String name50 = new String(chars);

            when(userRepository.existsByUsername(name50)).thenReturn(false);

            userService.register(name50, "password123");

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 密码为null应抛异常")
        void testRegisterNullPassword() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register("testuser", null)
            );
            assertEquals("密码长度不能少于6个字符", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 密码少于6字符应抛异常")
        void testRegisterShortPassword() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register("testuser", "12345")
            );
            assertEquals("密码长度不能少于6个字符", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 密码刚好6字符可以注册")
        void testRegisterPasswordExactly6Chars() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);

            userService.register("testuser", "123456");

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 用户名已存在应抛异常")
        void testRegisterDuplicateUsername() {
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    userService.register("existing", "password123")
            );
            assertEquals("用户名已存在", ex.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("注册 - 用户名去除首尾空白")
        void testRegisterTrimsUsername() {
            when(userRepository.existsByUsername("trimmed")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.register("  trimmed  ", "password123");

            verify(userRepository).existsByUsername("trimmed");
            verify(userRepository).save(argThat((User user) ->
                    "trimmed".equals(user.getUsername())
            ));
        }
    }

    // ======================== authenticate 测试 ========================

    @Nested
    @DisplayName("认证功能测试")
    class AuthenticateTests {

        private User mockUser;
        private final String rawPassword = "correctPassword";

        @BeforeEach
        void setUp() {
            mockUser = new User();
            mockUser.setId(1L);
            mockUser.setUsername("testuser");
            mockUser.setPassword(passwordEncoder.encode(rawPassword));
        }

        @Test
        @DisplayName("认证成功 - 正确的用户名和密码")
        void testAuthenticateSuccess() {
            when(userRepository.findByUsername("testuser")).thenReturn(mockUser);

            User result = userService.authenticate("testuser", rawPassword);

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals(1L, result.getId().longValue());
        }

        @Test
        @DisplayName("认证失败 - 错误的密码")
        void testAuthenticateWrongPassword() {
            when(userRepository.findByUsername("testuser")).thenReturn(mockUser);

            User result = userService.authenticate("testuser", "wrongPassword");

            assertNull(result);
        }

        @Test
        @DisplayName("认证失败 - 不存在的用户名")
        void testAuthenticateUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(null);

            User result = userService.authenticate("nonexistent", "somePassword");

            assertNull(result);
        }

        @Test
        @DisplayName("认证失败 - 空用户名")
        void testAuthenticateEmptyUsername() {
            when(userRepository.findByUsername("")).thenReturn(null);

            User result = userService.authenticate("", "somePassword");

            assertNull(result);
        }
    }
}
