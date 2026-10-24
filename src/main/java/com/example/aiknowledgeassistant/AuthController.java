package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.ApiResponse;
import com.example.aiknowledgeassistant.dto.LoginRequest;
import com.example.aiknowledgeassistant.dto.LoginResponse;
import com.example.aiknowledgeassistant.dto.UserProfile;
import com.example.aiknowledgeassistant.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class AuthController {
    static final String CURRENT_USER = AuthController.class.getName() + ".currentUser";
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // With no cross-origin CORS permission, requiring this non-simple header
    // prevents browser cross-site form submissions to login and logout.
    private void requireAjaxHeader(String header) {
        if (!"XMLHttpRequest".equals(header)) {
            throw new IllegalArgumentException("请提供X-Requested-With: XMLHttpRequest请求头");
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest body,
            @RequestHeader(value = "X-Requested-With", defaultValue = "") String header,
            HttpServletRequest request) {
        requireAjaxHeader(header);
        UserProfile user = userService.authenticate(body);
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute(CURRENT_USER, user);
        return new LoginResponse(true, "登录成功", user);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(CURRENT_USER) instanceof UserProfile user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "请先登录"));
        }
        return ResponseEntity.ok(new LoginResponse(true, "获取成功", user));
    }

    @PostMapping("/logout")
    public ApiResponse logout(
            @RequestHeader(value = "X-Requested-With", defaultValue = "") String header,
            HttpServletRequest request) {
        requireAjaxHeader(header);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new ApiResponse(true, "退出成功");
    }
}
