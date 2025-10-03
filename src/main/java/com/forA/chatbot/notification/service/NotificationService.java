package com.forA.chatbot.notification.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.NotificationHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.notification.domain.DeviceToken;
import com.forA.chatbot.notification.dto.TokenRefreshRequestDto;
import com.forA.chatbot.notification.repository.DeviceTokenRepository;
import com.forA.chatbot.user.domain.User;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final UserRepository userRepository; // 사용자 조회를 위한 레포지토리

  @Transactional
  public void saveOrUpdateToken(Long userId, TokenRefreshRequestDto requestDto) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotificationHandler(ErrorStatus.USER_NOT_FOUND));

    deviceTokenRepository
        .findByDeviceToken(requestDto.getToken())
        .ifPresentOrElse(
            deviceToken ->
                log.info("Token already exists for user: {}. Updating timestamp.", userId),
            () -> {
              log.info("Registering new token for user: {}", userId);
              DeviceToken newDeviceToken = new DeviceToken(user, requestDto.getToken());
              deviceTokenRepository.save(newDeviceToken);
            });
  }

  public void sendPushNotification(String targetToken, String title, String body) {
    Message message =
        Message.builder()
            .setToken(targetToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build();

    try {
      FirebaseMessaging.getInstance().send(message);
    } catch (FirebaseMessagingException e) {
      String errorCode = e.getMessagingErrorCode().toString();
      if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
        log.warn("Deleting invalid FCM token: {}", targetToken);
        deviceTokenRepository
            .findByDeviceToken(targetToken)
            .ifPresent(deviceTokenRepository::delete);
      } else {
        log.error("FCM message sending failed", e);
        throw new NotificationHandler(ErrorStatus._FCM_SEND_ERROR);
      }
    }
  }
}
