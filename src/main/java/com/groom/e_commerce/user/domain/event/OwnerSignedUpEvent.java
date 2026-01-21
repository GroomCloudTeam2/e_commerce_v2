package com.groom.e_commerce.user.domain.event;

import java.util.UUID;

public record OwnerSignedUpEvent(UUID userId, UUID ownerId, String email, String storeName) {
}
