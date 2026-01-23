package org.example.review.infrastructure.redis;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReviewRedisRepository {

    private final RedisTemplate<String, Object> reviewRedisTemplate;

    /**
     * 리뷰 저장
     * - ZSET 인덱스
     * - 리뷰 데이터
     */
    public void save(ReviewReadModel review) {
        String indexKey = ReviewRedisKey.productReviewIndex(review.getProductId());
        String dataKey = ReviewRedisKey.reviewData(review.getReviewId());

        long score = review.getCreatedAt()
            .toEpochSecond(ZoneOffset.UTC);

        // 리뷰 데이터 저장
        reviewRedisTemplate.opsForValue().set(dataKey, review);

        // 상품 리뷰 인덱스에 추가
        reviewRedisTemplate.opsForZSet()
            .add(indexKey, review.getReviewId().toString(), score);
    }

    /**
     * 상품별 리뷰 조회 (최신순)
     */
    public List<ReviewReadModel> findByProductId(
        UUID productId,
        int page,
        int size
    ) {
        String indexKey = ReviewRedisKey.productReviewIndex(productId);

        int start = page * size;
        int end = start + size - 1;

        Set<Object> reviewIds =
            reviewRedisTemplate.opsForZSet()
                .reverseRange(indexKey, start, end);

        if (reviewIds == null || reviewIds.isEmpty()) {
            return List.of();
        }

        List<ReviewReadModel> results = new ArrayList<>();

        for (Object reviewId : reviewIds) {
            String dataKey = ReviewRedisKey.reviewData(
                UUID.fromString(reviewId.toString())
            );
            ReviewReadModel review =
                (ReviewReadModel) reviewRedisTemplate.opsForValue().get(dataKey);

            if (review != null) {
                results.add(review);
            }
        }

        return results;
    }

    /**
     * 리뷰 삭제
     */
    public void delete(UUID productId, UUID reviewId) {
        String indexKey = ReviewRedisKey.productReviewIndex(productId);
        String dataKey = ReviewRedisKey.reviewData(reviewId);

        reviewRedisTemplate.opsForZSet()
            .remove(indexKey, reviewId.toString());

        reviewRedisTemplate.delete(dataKey);
    }
}
