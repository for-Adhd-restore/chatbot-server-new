package com.forA.chatbot.subscriptions.dto;

import lombok.Getter;

@Getter
public class SubscriptionVerificationRequest {
  private String transactionId; // iOS 앱(StoreKit 2)에서 받은 최신 transaction ID
  private String productId; // 구매 상품 ID (연간? or 월간)
}
