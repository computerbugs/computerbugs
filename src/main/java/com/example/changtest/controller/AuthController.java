package com.example.changtest.controller;

import com.example.changtest.entity.LoginLog;
import com.example.changtest.entity.User;
import com.example.changtest.repository.LoginLogRepository;
import com.example.changtest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final LoginLogRepository loginLogRepository;

    private static final String CSRF_TOKEN_ATTR = "csrfToken";
    private static final int MAX_IP_LENGTH = 45;

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        // 生成 CSRF token 防护跨站请求伪造
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_ATTR, csrfToken);
        model.addAttribute("csrfToken", csrfToken);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(value = "_csrf", required = false) String csrfToken,
                        HttpSession session,
                        HttpServletRequest request,
                        Model model) {
        // CSRF 校验
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (sessionToken == null || !sessionToken.equals(csrfToken)) {
            model.addAttribute("loginError", "请求无效，请刷新页面后重试");
            model.addAttribute("csrfToken", sessionToken);
            return "login";
        }

        User user = userService.authenticate(username, password);

        String ip = extractClientIp(request);

        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIpAddress(ip);

        if (user != null) {
            log.setSuccess(true);
            log.setMessage("登录成功");
            loginLogRepository.save(log);

            // 防止 Session Fixation：登录前销毁旧会话，创建新会话
            session.invalidate();
            HttpSession newSession = request.getSession(true);

            // 仅存储 userId 和 username，不将密码 hash 存入 session
            newSession.setAttribute("userId", user.getId());
            newSession.setAttribute("username", user.getUsername());
            return "redirect:/main";
        }

        log.setSuccess(false);
        log.setMessage("用户名或密码错误");
        loginLogRepository.save(log);
        model.addAttribute("loginError", "用户名或密码错误");
        model.addAttribute("username", username);
        model.addAttribute("csrfToken", session.getAttribute(CSRF_TOKEN_ATTR));
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        // 生成 CSRF token 防护跨站请求伪造
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_ATTR, csrfToken);
        model.addAttribute("csrfToken", csrfToken);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(value = "_csrf", required = false) String csrfToken,
                           HttpSession session,
                           Model model) {
        // CSRF 校验
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (sessionToken == null || !sessionToken.equals(csrfToken)) {
            model.addAttribute("registerError", "请求无效，请刷新页面后重试");
            model.addAttribute("csrfToken", sessionToken);
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("registerError", "两次输入的密码不一致");
            model.addAttribute("username", username);
            model.addAttribute("csrfToken", session.getAttribute(CSRF_TOKEN_ATTR));
            return "register";
        }
        try {
            userService.register(username, password);
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerError", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("csrfToken", session.getAttribute(CSRF_TOKEN_ATTR));
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    /**
     * 从请求中安全提取客户端 IP 地址。
     * 解析 X-Forwarded-For 头中的逗号分隔多 IP 列表，取第一个（最靠近客户端的代理 IP），
     * 对提取的 IP 进行格式校验，并截断到数据库字段长度限制。
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // X-Forwarded-For 可能包含多个 IP（逗号分隔），取第一个
            String[] parts = xff.split(",");
            for (String part : parts) {
                String candidate = part.trim();
                if (isValidIp(candidate)) {
                    return truncateIp(candidate);
                }
            }
        }
        // 回退到 RemoteAddr
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && isValidIp(remoteAddr)) {
            return truncateIp(remoteAddr);
        }
        return "unknown";
    }

    /**
     * 简单的 IP 地址格式校验（IPv4 和 IPv6）。
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // IPv4: x.x.x.x
        if (ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return true;
        }
        // IPv6: 包含冒号的地址
        if (ip.contains(":")) {
            return ip.matches("^[0-9a-fA-F:]+$");
        }
        return false;
    }

    /**
     * 截断 IP 地址到数据库字段允许的最大长度。
     */
    private String truncateIp(String ip) {
        if (ip.length() > MAX_IP_LENGTH) {
            return ip.substring(0, MAX_IP_LENGTH);
        }
        return ip;
    }
}
