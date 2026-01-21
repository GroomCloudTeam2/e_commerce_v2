package com.groom.e_commerce.review.infrastructure.redis;

import java.util.UUID;

public final class ReviewRedisKey {

    private ReviewRedisKey() {}

    /**
     * 상품별 리뷰 인덱스 (ZSET)
     * score = createdAt epoch seconds
     * value = reviewId
     */
    public static String productReviewIndex(UUID productId) {
        return "review:product:" + productId;
    }

    /**
     * 리뷰 단건 데이터
     */
    public static String reviewData(UUID reviewId) {
        return "review:data:" + reviewId;
    }
}
