package com.forA.chatbot.auth.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.AuthHandler;
import com.forA.chatbot.auth.domain.RefreshToken;
import com.forA.chatbot.auth.dto.RefreshTokenRequest;
import com.forA.chatbot.auth.dto.RefreshTokenResponse;
import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.auth.repository.RefreshTokenRepository;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.user.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request) {
    String refreshTokenValue = request.getRefreshToken();


    // 1. RefreshToken 유효성 검증
    RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
        .orElseThrow(() -> new AuthHandler(ErrorStatus.TOKEN_REFRESH_FAILED));

    // 2. 만료 확인
    if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      refreshTokenRepository.delete(refreshToken);
      throw new AuthHandler(ErrorStatus.EXPIRED_REFRESH_TOKEN);
    }

    // 3. 사용자 정보 조회
    User user = userRepository.findById(refreshToken.getUserId())
        .orElseThrow(() -> new AuthHandler(ErrorStatus.USER_NOT_FOUND));

    // 4. 새로운 Access Token 생성
    String newAccessToken = jwtUtil.createAccessToken(String.valueOf(refreshToken.getUserId()));

    log.info("Access Token 갱신 완료: userId={}", refreshToken.getUserId());

    boolean hasNickname = user.getNickname() != null && !user.getNickname().isEmpty();

    return RefreshTokenResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshTokenValue)
        .hasNickname(hasNickname)
        .nickname(user.getNickname())
        .build();
  }

}
