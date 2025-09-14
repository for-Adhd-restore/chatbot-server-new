package com.forA.chatbot.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

  private final Key secretKey;
  private final long accessTokenValidityInMilliseconds;

  public JwtUtil(@Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration_time}") long expirationTime) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = expirationTime;
  }

  /*
   * 사용자 ID를 받아 Access Token을 생성합니다.
   */
  public String createAccessToken(String userId) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + this.accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .setSubject(userId)
        .setIssuedAt(now)
        .setExpiration(validity)
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /*
   * RefreshToken 생성,
   */
  public String createRefreshToken() {
    return "rt_" + UUID.randomUUID().toString().replace("-", "");
  }

}
