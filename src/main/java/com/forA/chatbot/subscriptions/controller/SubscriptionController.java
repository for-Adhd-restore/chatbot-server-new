package com.forA.chatbot.subscriptions.controller;

import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.subscriptions.dto.SubscriptionVerificationRequest;
import com.forA.chatbot.subscriptions.service.SubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/subscription")
public class SubscriptionController {
  private final SubscriptionService subscriptionService;
  /**
   * [클라이언트 -> 서버]
   * 사용자가 앱에서 최초 구매(또는 복원) 후 영수증(transactionId)을 전송하는 API
   * */
  @PostMapping("/verify")
  public void verifyPurchase(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody SubscriptionVerificationRequest request
  ) {
    // 1. 현재 인증된 사용자 (userDetails.getUserId())조회
    Long userId = userDetails.getUserId();
    // SubscriptionService의 verifyPurchase 메서드 호출 (사용자 정보, ios 맵이 보낸 transactionId 전달)
    subscriptionService.verifyPurchase(userId, request);
  }

  /**
   * [Apple 서버 -> 우리 서버]
   * Apple 구독 상태 변경(갱신, 만료, 취소, 환불 등)을 알려주는 Webhook API
   * (이 API는 Apple만 호출하므로 우리 앱의 JWT 인증(SecurityConfig)에서 제외 필요) <- Why??
   * */
    @PostMapping("/apple-webhook")
    public void handleAppleWebhook(@RequestBody String jwsPayload) {
      // 1. Apple이 보낸 암호화된 JWS 페이로드를 그대로 받음

      // 2. SubscriptionService의 handleAppleWebhook 메서드로 전달
      subscriptionService.handleAppleWebhook(jwsPayload);
    }

}
