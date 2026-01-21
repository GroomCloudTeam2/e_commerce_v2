package com.groom.e_commerce.user.domain.event;

import java.util.UUID;

import com.groom.e_commerce.user.domain.entity.user.UserRole;

public record UserSignedUpEvent(UUID userId, String email, UserRole role) {
}
