package com.example.changtest.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/login", "/register", "/logout",
            "/css", "/js", "/images", "/webjars",
            "/favicon.ico"
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(httpRequest.getContextPath().length());

        // Allow public paths
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/") || path.startsWith(publicPath + ".")) {
                chain.doFilter(request, response);
                return;
            }
        }

        // Check session for logged-in user (stored as userId, not the full User entity)
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            chain.doFilter(request, response);
        } else {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
        }
    }

    @Override
    public void destroy() {
    }
}
