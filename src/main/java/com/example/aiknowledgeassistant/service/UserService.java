package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.RegisterRequest;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.mapper.UserMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void register(RegisterRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String username = request.username().strip();
        if (username.length() > 50) {
            throw new IllegalArgumentException("用户名不能超过50个字符");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (request.password().length() < 8
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密码至少8个字符，且不能超过72个UTF-8字节");
        }
        String email = request.email() == null ? null : request.email().strip();
        if (email != null && !email.isEmpty()
                && (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(email == null || email.isEmpty() ? null : email);
        if (userMapper.insert(user) != 1) {
            throw new IllegalStateException("注册写入失败");
        }
    }
}
