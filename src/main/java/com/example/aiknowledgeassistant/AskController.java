package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.security.SessionUser;
import com.example.aiknowledgeassistant.service.AskService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AskController {
    private final AskService service;
    public AskController(AskService service) { this.service = service; }
    public record Question(String question) {}
    @GetMapping("/status")
    public Map<String,Object> status() {
        return Map.of("version", "1.0.0", "aiConfigured", service.aiEnabled());
    }
    @PostMapping("/ask")
    public AskService.Answer ask(@RequestBody Question body, HttpServletRequest request) {
        return service.ask(SessionUser.require(request).id(), body == null ? null : body.question());
    }
}
