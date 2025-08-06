package com.javacafe.realtime_sujeong.user.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record UserResponse(
        String email,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
