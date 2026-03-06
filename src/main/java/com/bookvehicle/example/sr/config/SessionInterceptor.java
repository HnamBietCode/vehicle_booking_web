package com.bookvehicle.example.sr.config;

import com.bookvehicle.example.sr.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Kiểm tra xác thực và phân quyền cho mỗi request.
 * - /admin/**   → phải là ADMIN
 * - /profile/** → phải đăng nhập
 * - /premium/** → phải đăng nhập
 */
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI();

        // Các path công khai – không cần kiểm tra
        if (path.startsWith("/auth") || path.startsWith("/css") ||
                path.startsWith("/js") || path.startsWith("/images") ||
                path.equals("/") || path.startsWith("/favicon") ||
                path.startsWith("/vehicles")) {   // trang tìm xe là công khai
            return true;
        }

        User loggedUser = SecurityUtil.getLoggedUser(request.getSession(false));

        // Chưa đăng nhập
        if (loggedUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login?redirect=" +
                    java.net.URLEncoder.encode(path, "UTF-8"));
            return false;
        }

        // Bảo vệ /admin/**
        if (path.startsWith("/admin") && !loggedUser.getRole().name().equals("ADMIN")) {
            response.sendRedirect(request.getContextPath() + "/auth/login?error=forbidden");
            return false;
        }

        // Thêm user vào request attribute để Thymeleaf truy cập dễ hơn
        request.setAttribute("loggedUser", loggedUser);
        return true;
    }
}
