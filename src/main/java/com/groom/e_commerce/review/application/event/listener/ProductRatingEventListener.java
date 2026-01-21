package com.groom.e_commerce.review.application.event.listener;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.groom.e_commerce.review.application.event.ReviewCreatedEvent;
import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductRatingEventListener {

    private final ProductRatingRepository productRatingRepository;

    @Async("eventExecutor")
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReviewCreatedEvent event) {

        ProductRatingEntity rating =
            productRatingRepository.findByProductId(event.productId())
                .orElseGet(() -> new ProductRatingEntity(event.productId()));

        rating.updateRating(event.rating());
        productRatingRepository.save(rating);
    }
}
