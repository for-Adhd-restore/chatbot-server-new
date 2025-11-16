package com.forA.chatbot.subscriptions.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.subscriptions.dto.SubscriptionResponseDto;
import com.forA.chatbot.subscriptions.dto.SubscriptionVerifyRequest;
import com.forA.chatbot.subscriptions.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {
  private final SubscriptionService subscriptionService;
  /**
   * [클라이언트 -> 서버]
   * 사용자가 앱에서 최초 구매(또는 복원) 후 영수증(transactionId)을 전송하는 API
   * */
  @PostMapping("/verify")
  public ApiResponse<SubscriptionResponseDto> verifySubscription(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody SubscriptionVerifyRequest request
  ) {
    Long userId = userDetails.getUserId();
    log.info("영수증 검증 요청 수신 - userId = {}", userId);
    SubscriptionResponseDto responseDto = subscriptionService.verifySubscription(userId, request);
    return ApiResponse.onSuccess(responseDto);
  }
}
