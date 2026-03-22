package com.bookvehicle.example.sr.config;

import com.bookvehicle.example.sr.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Tiện ích lấy User đang đăng nhập từ HttpSession.
 */
public class SecurityUtil {

    public static final String SESSION_KEY = "loggedUser";

    private SecurityUtil() {}

    public static User getLoggedUser(HttpSession session) {
        if (session == null) return null;
        return (User) session.getAttribute(SESSION_KEY);
    }

    public static User getLoggedUser(HttpSession session, HttpServletRequest request) {
        User u = getLoggedUser(session);
        if (u != null) return u;
        Object attr = request.getAttribute(SESSION_KEY);
        if (attr instanceof User user) {
            return user;
        }
        return null;
    }

    public static boolean isLoggedIn(HttpSession session) {
        return getLoggedUser(session) != null;
    }

    public static boolean isAdmin(HttpSession session) {
        User u = getLoggedUser(session);
        return u != null && u.getRole() != null &&
               u.getRole().name().equals("ADMIN");
    }

    public static boolean isCustomer(HttpSession session) {
        User u = getLoggedUser(session);
        return u != null && u.getRole() != null &&
               u.getRole().name().equals("CUSTOMER");
    }

    public static boolean isDriver(HttpSession session) {
        User u = getLoggedUser(session);
        return u != null && u.getRole() != null &&
               u.getRole().name().equals("DRIVER");
    }

    public static void setLoggedUser(HttpSession session, User user) {
        session.setAttribute(SESSION_KEY, user);
    }

    public static void clearSession(HttpSession session) {
        session.invalidate();
    }
}
