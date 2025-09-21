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
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration_time}") long expirationTime) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = expirationTime;
  }

  /**
   * 사용자 ID를 받아 Access Token을 생성합니다. (최신 방법)
   */
  public String createAccessToken(String userId) {
    Instant now = Instant.now();
    Instant expiration = now.plus(accessTokenValidityInMilliseconds, ChronoUnit.MILLIS);

    return Jwts.builder()
        .subject(userId)                    // setSubject → subject
        .issuedAt(Date.from(now))          // setIssuedAt → issuedAt
        .expiration(Date.from(expiration)) // setExpiration → expiration
        .signWith(secretKey)               // signWith 간소화 (알고리즘 자동 선택)
        .compact();
  }

  /*
   * RefreshToken 생성,
   */
  public String createRefreshToken() {
    return "rt_" + UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * HttpServletRequest에서 Bearer 토큰을 추출하고 사용자 ID를 반환
   */
  public Long getUserIdFromRequest(HttpServletRequest request) {
    String token = extractTokenFromRequest(request);
    return getUserIdFromToken(token);
  }

  /**
   * HttpServletRequest에서 Bearer 토큰 추출
   */
  public String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    log.error("Authorization 헤더가 없거나 올바르지 않습니다");
    throw new GeneralException(ErrorStatus._UNAUTHORIZED);
  }

  /**
   * JWT 토큰에서 사용자 ID 추출 (최신 방법)
   */
  public Long getUserIdFromToken(String token) {
    try {
      Claims claims = Jwts.parser()              // parserBuilder() → parser()
          .verifyWith(secretKey)             // setSigningKey → verifyWith
          .build()
          .parseSignedClaims(token)          // parseClaimsJws → parseSignedClaims
          .getPayload();                     // getBody() → getPayload()

      return Long.valueOf(claims.getSubject());

    } catch (SecurityException | MalformedJwtException e) {
      log.error("잘못된 JWT 서명입니다", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    } catch (ExpiredJwtException e) {
      log.error("만료된 JWT 토큰입니다", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    } catch (UnsupportedJwtException e) {
      log.error("지원되지 않는 JWT 토큰입니다", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    } catch (IllegalArgumentException e) {
      log.error("JWT 토큰이 잘못되었습니다", e);
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    }
  }

  /**
   * JWT 토큰 유효성 검증 (최신 방법)
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token);

      log.debug("JWT 토큰 검증 성공");
      return true;

    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }
}
