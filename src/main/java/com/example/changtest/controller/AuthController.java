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

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final LoginLogRepository loginLogRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        HttpServletRequest request,
                        Model model) {
        User user = userService.authenticate(username, password);

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIpAddress(ip);

        if (user != null) {
            log.setSuccess(true);
            log.setMessage("登录成功");
            loginLogRepository.save(log);
            session.setAttribute("user", user);
            return "redirect:/main";
        }

        log.setSuccess(false);
        log.setMessage("用户名或密码错误");
        loginLogRepository.save(log);
        model.addAttribute("loginError", "用户名或密码错误");
        model.addAttribute("username", username);
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("registerError", "两次输入的密码不一致");
            model.addAttribute("username", username);
            return "register";
        }
        try {
            userService.register(username, password);
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerError", e.getMessage());
            model.addAttribute("username", username);
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}
