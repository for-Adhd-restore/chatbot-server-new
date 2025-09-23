package com.forA.chatbot.auth.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.AuthHandler;
import com.forA.chatbot.auth.dto.AppleLoginRequest;
import com.forA.chatbot.auth.dto.AuthResponse;
import com.forA.chatbot.auth.dto.RefreshTokenRequest;
import com.forA.chatbot.auth.dto.RefreshTokenResponse;
import com.forA.chatbot.auth.service.AppleAuthService;
import com.forA.chatbot.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AppleAuthService appleAuthService;
  private final AuthService authService;

  @PostMapping("/apple")
  public ApiResponse<AuthResponse> appleLogin(@RequestBody AppleLoginRequest request) {
    try {
      log.info("Apple 로그인 요청 수신");
      AuthResponse response = appleAuthService.authenticateWithApple(request);
      log.info("Apple 로그인 성공: userId={}, isNewUser={}", response.getUserId(), response.isNewUser());

      return ApiResponse.onSuccess(response);
    } catch (Exception e) {
      log.error("Apple 로그인 실패", e);
      throw new AuthHandler(ErrorStatus.APPLE_TOKEN_INVALID);
    }
  }

  @PostMapping("/refresh")
  public ApiResponse<RefreshTokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    try {
      log.info("토큰 재발급 요청 수신");
      RefreshTokenResponse response = authService.refreshAccessToken(request);
      log.info("토큰 재발급 성공");
      return ApiResponse.onSuccess(response);
    } catch (IllegalArgumentException e) {
      log.error("토큰 재발급 실패: {}", e.getMessage());
      // Note: This requires TOKEN_REFRESH_FAILED to be defined in ErrorStatus
      throw new AuthHandler(ErrorStatus.TOKEN_REFRESH_FAILED);
    }
  }


}
