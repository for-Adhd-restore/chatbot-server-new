package com.forA.chatbot.auth.service;

import com.forA.chatbot.auth.domain.RefreshToken;
import com.forA.chatbot.auth.dto.AuthResponse;
import com.forA.chatbot.auth.dto.TempLoginRequest;
import com.forA.chatbot.auth.dto.TempRefreshTokenRequest;
import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.auth.repository.RefreshTokenRepository;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TempAuthService {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public AuthResponse tempLogin(TempLoginRequest request) {
    log.info("임시 로그인 시도: {}", request.getEmail());
    System.out.println("임시 로그인 시도: "+request.getEmail());
    // 1. 기존 사용자 조회 또는 신규 생성 (개발용이므로 간단하게)
    Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
    User user;
    boolean isNewUser = false;

    if (existingUser.isPresent()) {
      log.info("기존 사용자 로그인: {}", request.getEmail());
      user = existingUser.get();
    } else {
      log.info("신규 사용자 생성: {}", request.getEmail());
      isNewUser = true;

      // 간단한 임시 사용자 생성
      user = User.builder()
          .email(request.getEmail())
          .fullName("김눈송") // 기본값
          .firstName("눈송")
          .lastName("김")
          .build();

      user = userRepository.save(user);
    }

    // 2. JWT 토큰 생성
    String accessToken = jwtUtil.createAccessToken(String.valueOf(user.getId()));
    String refreshToken = createRefreshToken(user.getId());

    // 3. 응답 생성
    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(3600L)
        .userId(user.getId())
        .isNewUser(isNewUser)
        .build();
  }

  @Transactional
  public AuthResponse refreshAccessToken(TempRefreshTokenRequest request) {
    String refreshTokenValue = request.getRefreshToken();

    // 1. RefreshToken 유효성 검증
    Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(refreshTokenValue);
    if (refreshTokenOpt.isEmpty()) {
      throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다");
    }

    RefreshToken refreshToken = refreshTokenOpt.get();

    // 2. 만료 확인
    if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      refreshTokenRepository.delete(refreshToken);
      throw new IllegalArgumentException("만료된 리프레시 토큰입니다");
    }

    // 3. 새로운 Access Token 생성
    String newAccessToken = jwtUtil.createAccessToken(String.valueOf(refreshToken.getUserId()));

    log.info("Access Token 갱신 완료: userId={}", refreshToken.getUserId());

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(refreshTokenValue) // 기존 RefreshToken 재사용
        .tokenType("Bearer")
        .expiresIn(3600L)
        .userId(refreshToken.getUserId())
        .isNewUser(false)
        .build();
  }

  @Transactional
  public void logout(Long userId) {
    log.info("로그아웃: userId={}", userId);
    refreshTokenRepository.deleteByUserId(userId);
  }

  // 기존 createRefreshToken 메서드 그대로 활용
  private String createRefreshToken(Long userId) {
    Optional<RefreshToken> existingRefreshToken = refreshTokenRepository.findByUserId(userId);
    if (existingRefreshToken.isPresent()) {
      refreshTokenRepository.deleteByUserId(userId);
    }

    String createRefreshToken = jwtUtil.createRefreshToken();

    // RefreshToken DB 저장
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(createRefreshToken)
            .userId(userId)
            .expiresAt(LocalDateTime.now().plusMonths(3))
            .build()
    );

    return createRefreshToken;
  }
}