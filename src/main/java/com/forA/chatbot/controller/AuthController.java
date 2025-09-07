package com.forA.chatbot.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.AuthHandler;
import com.forA.chatbot.dto.auth.AppleLoginRequest;
import com.forA.chatbot.dto.auth.AuthResponse;
import com.forA.chatbot.service.auth.AppleAuthService;
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

    @PostMapping("/apple")
    public ApiResponse<AuthResponse> appleLogin(@RequestBody AppleLoginRequest request) {
        try {
            log.info("Apple 로그인 요청 수신");
            AuthResponse response = appleAuthService.authenticateWithApple(request);
            log.info("Apple 로그인 성공: userId={}, isNewUser={}",
                    response.getUserId(),
                    response.isNewUser());

            return ApiResponse.onSuccess(response);
        } catch (Exception e) {
            log.error("Apple 로그인 실패", e);
            throw new AuthHandler(ErrorStatus.APPLE_TOKEN_INVALID);
        }

    }
}
