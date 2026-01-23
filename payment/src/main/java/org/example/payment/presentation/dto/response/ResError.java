package org.example.payment.presentation.dto.response;

import java.time.LocalDateTime;

public record ResError(
	String code,
	String message,
	LocalDateTime timestamp
) {
	public static ResError of(String code, String message) {
		return new ResError(code, message, LocalDateTime.now());
	}
}
