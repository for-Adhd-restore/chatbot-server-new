package com.forA.chatbot.subscriptions.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.SubscriptionHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.config.AppleClientSecretGenerator;
import com.forA.chatbot.enums.SubscriptionStatus;
import com.forA.chatbot.subscriptions.client.AppleAppStoreClient;
import com.forA.chatbot.subscriptions.domain.Subscription;
import com.forA.chatbot.subscriptions.dto.DecodedSignedRenewalInfo;
import com.forA.chatbot.subscriptions.dto.DecodedSignedTransactionInfo;
import com.forA.chatbot.subscriptions.dto.SubscriptionResponseDto;
import com.forA.chatbot.subscriptions.dto.SubscriptionStatusResponse;
import com.forA.chatbot.subscriptions.dto.SubscriptionStatusResponse.LastTransaction;
import com.forA.chatbot.subscriptions.dto.SubscriptionVerifyRequest;
import com.forA.chatbot.subscriptions.repository.SubscriptionRepository;
import com.forA.chatbot.subscriptions.util.AppStoreJwsValidator;
import com.forA.chatbot.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final AppleAppStoreClient appleAppStoreClient;
  private final AppleClientSecretGenerator appleClientSecretGenerator;
  private final AppStoreJwsValidator jwsValidator;
  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  /**
   * ios 앱이 보낸 영수증 검증 -> 우리 DB에 구독 정보 생성/업데이트
   * */
  @Transactional
  public SubscriptionResponseDto verifySubscription(Long userId, SubscriptionVerifyRequest request) {
    String transactionId = request.getOriginalTransactionId();
    log.info("영수증 검증 시작 - userId = {}, transactionId = {}", userId, transactionId);

    // 애플 서버 인증용 JWT 생성
    String serverToServerJwt;
    try{
      serverToServerJwt = appleClientSecretGenerator.generate();
    } catch (Exception e) {
      log.error("Apple Client Secret (JWT) 생성 실패", e);
      throw new SubscriptionHandler(ErrorStatus.IAP_APPLE_API_CALL_FAILED);
    }
    String bearerToken = "Bearer " + serverToServerJwt;

    // apple 과 통신
    SubscriptionStatusResponse appleResponse;
    try {
      appleResponse = appleAppStoreClient.getAllSubscriptionStatuses(bearerToken, transactionId);
    } catch (Exception e) {
      log.error("Apple App Store API 호출 실패", e);
      throw new SubscriptionHandler(ErrorStatus.IAP_APPLE_API_CALL_FAILED);
    }
    // 4. 응답에서 JWS 추출
    LastTransaction lastTransaction = extractLastTransaction(appleResponse, transactionId);

    String signedTransactionInfo = lastTransaction.getSignedTransactionInfo();
    String signedRenewalInfo = lastTransaction.getSignedRenewalInfo();
    Integer appleStatus = lastTransaction.getStatus();

    // 5. JWS 검증 및 디코딩
    log.debug("JWS 디코딩 시작...");
    DecodedSignedTransactionInfo transactionInfo = jwsValidator.decodeSignedTransaction(signedTransactionInfo);
    DecodedSignedRenewalInfo renewalInfo = jwsValidator.decodeRenewalInfo(signedRenewalInfo);
    log.debug("JWS 디코딩 성공: productId={}", transactionInfo.getProductId());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new SubscriptionHandler(ErrorStatus.USER_NOT_FOUND));
    // 6. 기존 유저 구독이 있으면 생성
    Subscription subscription = subscriptionRepository.findByOriginalTransactionId(transactionId)
        .orElseGet(() -> {
          log.info("새로운 구독입니다. DB에 생성: {}", transactionId);
          return Subscription.builder()
              .user(user)
              .originalTransactionId(transactionId)
              .build();
        });

    subscription.updateFromApple(
        transactionInfo.getProductId(),
        transactionInfo.getTransactionId(),
        transactionInfo.getExpiresDateAsLocalDateTime(),
        renewalInfo.getAutoRenewStatus() == 1,
        transactionInfo.isTrial(),
        convertAppleStatus(appleStatus)
    );
    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("구독 상태 DB 저장 완료. User: {}, Status: {}, ExpiresAt: {}",
        userId, savedSubscription.getStatus(), savedSubscription.getExpiresAt());
    // 7. 클라이언트에 보낼 응답 생성
    return SubscriptionResponseDto.builder()
        .subscriptionId(savedSubscription.getId())
        .productId(savedSubscription.getProductId())
        .status(savedSubscription.getStatus())
        .expiresAt(savedSubscription.getExpiresAt())
        .isTrial(savedSubscription.getIsTrial())
        .build();
  }
  /**
   * Apple 응답에서 최신 트랜잭션을 안전하게 추출하는 헬퍼 메서드
   */
  private LastTransaction extractLastTransaction(SubscriptionStatusResponse response, String originalTransactionId) {
    if (response == null || response.getData() == null || response.getData().isEmpty()) {
      log.warn("Apple 응답에 [data] 필드가 비어있습니다. originalTransactionId: {}", originalTransactionId);
      throw new SubscriptionHandler(ErrorStatus.IAP_APPLE_INVALID_TRANSACTION);
    }
    // getData().get(0) -> Mori 앱의 단일 구독 그룹
    SubscriptionStatusResponse.SubscriptionGroupData groupData = response.getData().get(0);
    if (groupData == null || groupData.getLastTransactions() == null || groupData.getLastTransactions().isEmpty()) {
      log.warn("Apple 응답에 [lastTransactions] 필드가 비어있습니다. originalTransactionId: {}", originalTransactionId);
      throw new SubscriptionHandler(ErrorStatus.IAP_APPLE_INVALID_TRANSACTION);
    }
    SubscriptionStatusResponse.LastTransaction lastTransaction = groupData.getLastTransactions().get(0);

    if (lastTransaction.getSignedTransactionInfo() == null || lastTransaction.getSignedRenewalInfo() == null) {
      log.error("JWS 데이터가 null입니다. DTO의 'signedTransactionInfo' 필드명이 Apple JSON 키와 일치하는지 확인하세요.");
      throw new SubscriptionHandler(ErrorStatus.IAP_APPLE_INVALID_TRANSACTION);
    }
    return lastTransaction;
  }

  /**
   * Apple의 숫자 상태를 우리 Enum으로 변환하는 헬퍼 메서드 (이전 단계와 동일)
   */
  private SubscriptionStatus convertAppleStatus(Integer appleStatus) {
    // ... (이전 단계 6-4와 동일)
    if (appleStatus == null) {
      return SubscriptionStatus.EXPIRED;
    }
    switch (appleStatus) {
      case 1: return SubscriptionStatus.ACTIVE;
      case 2: return SubscriptionStatus.EXPIRED;
      case 3: return SubscriptionStatus.ACTIVE; // (Billing Retry)
      case 4: return SubscriptionStatus.ACTIVE; // (Grace Period)
      case 5: return SubscriptionStatus.CANCELLED; // (Revoked)
      default: return SubscriptionStatus.EXPIRED;
    }
  }
  /**
   * 알림 처리 & 구독 상태 실시간 업데이트
   * */
  public void handleAppleWebhook(String signedNotification) {
// 1. Apple의 공개키를 이용해 'signedNotification (JWS)' 서명 검증 및 디코딩

    // 2. 디코딩된 페이로드(SignedPayload)를 DTO로 파싱

    // 3. 알림 타입(notificationType)과 하위 타입(subtype) 추출
    //    (예: "SUBSCRIBED", "DID_RENEW", "EXPIRED", "REFUND", "REVOKE")

    // 4. 페이로드의 data 객체에서 'signedTransactionInfo' (또다른 JWS) 추출

    // 5. 'signedTransactionInfo'도 디코딩하여 거래 내역(DecodedTransaction) DTO로 파싱

    // 6. DTO에서 originalTransactionId 추출

    // 7. subscriptionRepository.findByOriginalTransactionId(originalTransactionId)로 구독 정보 조회
    //    (Webhook은 신규 구독자가 아닌 기존 구독자에 대한 변경이므로, 조회가 되어야 함)

    // 8. notificationType에 따라 분기 처리 (switch 문)

    //    case "SUBSCRIBED": // 신규 구독 (무료 체험 시작 등)
    //        // verifyPurchase와 유사하게 Subscription 객체 생성 또는 업데이트
    //        // status를 ACTIVE로 설정

    //    case "DID_RENEW": // 자동 갱신 성공(구독 연장)
    //        // Subscription의 expiresAt (만료일)을 새 만료일로 업데이트
    //        // status를 ACTIVE로 설정

    //    case "EXPIRED": // 구독 만료
    //        // Subscription의 status를 EXPIRED로 설정

    //    case "REFUND": // 환불
    //    case "REVOKE": // Apple 지원팀의 구독 취소
    //        // Subscription의 status를 CANCELLED로 설정

    //    case "DID_FAIL_TO_RENEW": // 갱신 실패 (결제 문제)
    //        // Subscription의 status를 PENDING (유예 기간) 등으로 설정 (선택적)

    // 9. 변경된 Subscription 객체를 repository.save()로 DB에 저장

  }
}
