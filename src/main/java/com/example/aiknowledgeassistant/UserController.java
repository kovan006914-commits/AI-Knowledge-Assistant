package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.ApiResponse;
import com.example.aiknowledgeassistant.dto.RegisterRequest;
import com.example.aiknowledgeassistant.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "注册成功"));
    }
}
