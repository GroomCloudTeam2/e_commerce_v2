package com.groom.e_commerce.user.domain.event;

import java.util.UUID;

public record UserWithdrawnEvent(UUID userId) {
}
