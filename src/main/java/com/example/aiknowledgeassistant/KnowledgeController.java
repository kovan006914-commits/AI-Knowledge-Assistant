package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.ApiResponse;
import com.example.aiknowledgeassistant.dto.KnowledgeRequest;
import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import com.example.aiknowledgeassistant.security.SessionUser;
import com.example.aiknowledgeassistant.service.KnowledgeService;
import com.example.aiknowledgeassistant.service.KnowledgeImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    private final KnowledgeImportService importService;
    public KnowledgeController(KnowledgeService service, KnowledgeImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    public record ImportResult(boolean success, int count, String message) {}

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importFile(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        int count = importService.importFile(SessionUser.require(request).id(), file);
        return ResponseEntity.status(201).body(new ImportResult(true, count, "文件导入成功"));
    }

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

