package com.groom.e_commerce.global.infrastructure.config.security;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-expiry:3600000}")
	private long accessTokenExpiry;

	@Value("${jwt.refresh-token-expiry:604800000}")
	private long refreshTokenExpiry;

	private SecretKey key;

	@PostConstruct
	public void init() {
		// ⚠️ Gateway와 완전히 동일한 방식!
		byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes());
		this.key = Keys.hmacShaKeyFor(keyBytes);
		log.info("JwtUtil initialized");
	}

	public String generateAccessToken(UUID userId, String email, String role) {
		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("email", email)
			.claim("role", role)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
			.signWith(key)
			.compact();
	}

	public String generateRefreshToken(UUID userId, String email, String role) {
		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("email", email)
			.claim("role", role)
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
			.signWith(key)
			.compact();
	}
}
