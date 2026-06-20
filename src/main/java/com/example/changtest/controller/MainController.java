package com.example.changtest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class MainController {

    @GetMapping("/main")
    public String mainPage(HttpSession session, Model model) {
        // 从 session 中读取 username（不再存储整个 User 实体，避免密码 hash 泄漏）
        String username = (String) session.getAttribute("username");
        if (username != null) {
            model.addAttribute("username", username);
        }
        return "main";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/main";
    }
}
