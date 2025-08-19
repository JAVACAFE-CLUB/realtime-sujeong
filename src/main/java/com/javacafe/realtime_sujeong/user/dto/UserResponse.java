package com.javacafe.realtime_sujeong.user.dto;

import java.time.LocalDateTime;

import com.javacafe.realtime_sujeong.user.entity.User;
import lombok.Builder;

@Builder
public record UserResponse(
        long id,
        String email,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse toEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
