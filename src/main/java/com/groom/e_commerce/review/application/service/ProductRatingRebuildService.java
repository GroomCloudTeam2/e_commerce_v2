package com.groom.e_commerce.review.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.e_commerce.review.domain.entity.ProductRatingEntity;
import com.groom.e_commerce.review.domain.entity.ReviewEntity;
import com.groom.e_commerce.review.domain.repository.ProductRatingRepository;
import com.groom.e_commerce.review.domain.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;


// 오류시 모든 리뷰를 새로 읽어서 평점을 다시 계산한다.
@Service
@RequiredArgsConstructor
public class ProductRatingRebuildService {

    private final ReviewRepository reviewRepository;
    private final ProductRatingRepository productRatingRepository;

    @Transactional
    public void rebuild(UUID productId) {

        List<ReviewEntity> reviews =
            reviewRepository.findAllByProductIdForRebuild(productId);

        ProductRatingEntity rating =
            productRatingRepository.findByProductId(productId)
                .orElseGet(() -> new ProductRatingEntity(productId));

        rating.reset();

        for (ReviewEntity review : reviews) {
            rating.updateRating(review.getRating());
        }

        productRatingRepository.save(rating);
    }
}
