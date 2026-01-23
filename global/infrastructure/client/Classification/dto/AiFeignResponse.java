package com.groom.e_commerce.global.infrastructure.client.Classification.dto;


public record AiFeignResponse(
	String category,
	double confidence
) {}
