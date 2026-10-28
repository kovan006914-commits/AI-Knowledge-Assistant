package com.example.aiknowledgeassistant.security;

import com.example.aiknowledgeassistant.AuthController;
import com.example.aiknowledgeassistant.dto.UserProfile;
import com.example.aiknowledgeassistant.exception.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionUser {
    private SessionUser() {}
    public static UserProfile require(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthController.CURRENT_USER) instanceof UserProfile user
                && user.id() != null) {
            return user;
        }
        throw new ApiProblem(401, "登录已过期，请重新登录");
    }
}
