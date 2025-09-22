package com.forA.chatbot.auth.jwt;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class JwtUtil {

  private final SecretKey secretKey;
  private final long accessTokenValidityInMilliseconds;

  public JwtUtil(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration_time}") long expirationTime
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = expirationTime;
  }

  /**
   * 사용자 ID 기반 Access Token 생성
   */
  public String createAccessToken(String userId) {
    Instant now = Instant.now();
    Instant expiration = now.plus(accessTokenValidityInMilliseconds, ChronoUnit.MILLIS);

    return Jwts.builder()
        .subject(userId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(secretKey)
        .compact();
  }

  /**
   * RefreshToken 생성
   */
  public String createRefreshToken() {
    return "rt_" + UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * HttpServletRequest에서 Bearer 토큰 추출
   */
  public String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    log.warn("Authorization 헤더가 없거나 올바르지 않습니다");
    return null; // 필터에서 null 체크 가능하도록 수정
  }

  /**
   * JWT 토큰에서 사용자 ID 추출
   */
  public Long getUserIdFromToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return Long.valueOf(claims.getSubject());

    } catch (ExpiredJwtException e) {
      log.error("만료된 JWT 토큰", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    } catch (JwtException | IllegalArgumentException e) {
      log.error("JWT 토큰 파싱 실패", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    }
  }

  /**
   * 토큰 만료 여부
   */
  public boolean isTokenExpired(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      return claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return true;
    }
  }

  /**
   * 토큰 발급 시간
   */
  public Date getIssuedAtFromToken(String token) {
    Claims claims = Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return claims.getIssuedAt();
  }

  /**
   * 토큰 유효성 검증
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token);

      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT 검증 실패: {}", e.getMessage());
      return false;
    }
  }
}