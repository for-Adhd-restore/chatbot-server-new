package com.forA.chatbot.auth.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.exception.handler.AuthHandler;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.auth.dto.AuthResponse;
import com.forA.chatbot.auth.dto.TempLoginRequest;
import com.forA.chatbot.auth.service.TempAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class TempAuthController {

  private final TempAuthService tempAuthService;


  @PostMapping("/temp-login")
  public ApiResponse<AuthResponse> tempLogin(@Valid @RequestBody TempLoginRequest request) {
    try {
      log.info("임시 로그인 요청 수신: email={}", request.getEmail());
      AuthResponse response = tempAuthService.tempLogin(request);
      log.info("임시 로그인 성공: userId={}, email={}",
          response.getUserId(), request.getEmail());

      return ApiResponse.onSuccess(response);
    } catch (IllegalArgumentException e) {
      log.error("사용자 없음: email={}, message={}", request.getEmail(), e.getMessage());
      throw new AuthHandler(ErrorStatus.USER_NOT_FOUND);
    } catch (Exception e) {
      log.error("임시 로그인 실패: email={}", request.getEmail(), e);
      throw new AuthHandler(ErrorStatus.TEMP_LOGIN_FAILED);
    }
  }
}