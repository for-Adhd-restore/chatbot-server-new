package com.forA.chatbot.auth.service;

import com.forA.chatbot.auth.domain.RefreshToken;
import com.forA.chatbot.auth.dto.AppleLoginRequest;
import com.forA.chatbot.auth.dto.AuthResponse;
import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.auth.repository.RefreshTokenRepository;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.user.domain.User;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppleAuthService {

  private final AppleTokenValidator appleTokenValidator;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public AuthResponse authenticateWithApple(AppleLoginRequest request) {
    // 1. Apple Identity Token 검증
    Claims claims = appleTokenValidator.validateToken(request.getIdentityToken());

    // 2. 애플 사용자 정보 추출
    String appleUniqueId = claims.getSubject();
    String email = claims.get("email", String.class);

    // 3. 기존 사용자 조회 또는 신규 생성
    Optional<User> existingUser = userRepository.findByAppleUniqueId(appleUniqueId);
    User user;
    boolean isNewUser = false;
    if (existingUser.isPresent()) {
      log.info("기존 사용자 가입 : {}", appleUniqueId);
      user = existingUser.get();
    } else {
      log.info("신규 사용자 가입 : {}", appleUniqueId);
      isNewUser = true;

      user =
          User.builder()
              .appleUniqueId(appleUniqueId)
              .email(email)
              .fullName(buildFullName(request.getFirstName(), request.getLastName()))
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
              .gender(Gender.UNKNOWN)
              .isDeleted(false)
              .isNotificationEnabled(false)
              .build();

      user = userRepository.save(user);
      log.info("신규 사용자 저장 완료 - userId: {}", user.getId());
    }
    // 4. JWT 토큰 생성
    String accessToken = jwtUtil.createAccessToken(String.valueOf(user.getId()));
    String refreshToken = createRefreshToken(user.getId());
    // 5. 응답 생성
    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(86400L)
        .userId(user.getId())
        .isNewUser(isNewUser)
        .build();
  }

  public String createRefreshToken(Long userId) {
    Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(userId);
    if (refreshToken.isPresent()) {
      refreshTokenRepository.deleteByUserId(userId);
    }
    String createRefreshToken = jwtUtil.createRefreshToken();
    // refreshToken 생성
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(createRefreshToken)
            .userId(userId)
            .expiresAt(LocalDateTime.now().plusMonths(6))
            .build());
    return createRefreshToken;
  }

  private String buildFullName(String firstName, String lastName) {
    if (firstName != null && lastName != null) {
      return firstName + " " + lastName;
    } else if (firstName != null) {
      return firstName;
    } else if (lastName != null) {
      return lastName;
    }
    return "Apple User"; // 기본값
  }
}
