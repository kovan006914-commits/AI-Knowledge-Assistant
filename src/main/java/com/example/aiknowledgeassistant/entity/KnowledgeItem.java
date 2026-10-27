package com.example.aiknowledgeassistant.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeItem {
    private Long id;
    private Long ownerId;
    private String title;
    private String content;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
