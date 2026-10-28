package com.example.aiknowledgeassistant.dto;

import java.io.Serializable;

public record UserProfile(Long id, String username, String email) implements Serializable {}
