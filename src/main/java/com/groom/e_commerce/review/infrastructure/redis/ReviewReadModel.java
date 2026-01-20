package com.groom.e_commerce.review.infrastructure.redis;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewReadModel {

    private UUID reviewId;
    private UUID productId;
    private UUID userId;
    private int rating;
    private String content;
    private LocalDateTime createdAt;
}
