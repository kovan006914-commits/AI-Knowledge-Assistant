package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.ApiResponse;
import com.example.aiknowledgeassistant.dto.KnowledgeRequest;
import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import com.example.aiknowledgeassistant.security.SessionUser;
import com.example.aiknowledgeassistant.service.KnowledgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    public KnowledgeController(KnowledgeService service) { this.service = service; }

    @GetMapping
    public KnowledgeService.Page list(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return service.list(SessionUser.require(request).id(), q, page, size);
    }
    @GetMapping("/{id}")
    public KnowledgeItem get(@PathVariable long id, HttpServletRequest request) {
        return service.get(SessionUser.require(request).id(), id);
    }
    @PostMapping
    public ResponseEntity<KnowledgeItem> create(@RequestBody KnowledgeRequest body, HttpServletRequest request) {
        return ResponseEntity.status(201).body(service.create(SessionUser.require(request).id(), body));
    }
    @PutMapping("/{id}")
    public KnowledgeItem update(@PathVariable long id, @RequestBody KnowledgeRequest body,
            HttpServletRequest request) {
        return service.update(SessionUser.require(request).id(), id, body);
    }
    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable long id, HttpServletRequest request) {
        service.delete(SessionUser.require(request).id(), id);
        return new ApiResponse(true, "已删除");
    }
}
