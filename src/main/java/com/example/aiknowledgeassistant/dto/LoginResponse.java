package com.example.aiknowledgeassistant.dto;

public record LoginResponse(boolean success, String message, UserProfile user) {}
