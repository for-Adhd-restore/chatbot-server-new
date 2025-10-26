package com.forA.chatbot.chat.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.chat.dto.ChatRequest;
import com.forA.chatbot.chat.dto.ChatResponse;
import com.forA.chatbot.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  /**
   * [1. 대화 시작/재개(이어하기)]
   * 챗봇 세션을 시작하거나 중간 이탈 시 재개합니다.
   * 사용자의 상태(onboardingCompleted)를 확인하고
   * 현재 단계(currentStep)에 맞는 첫 질문을 반환합니다.
   */
  @GetMapping("/session")
  public ApiResponse<ChatResponse> getCurrentStep(
      @AuthenticationPrincipal CustomUserDetails userDetails
  )
  {
    Long userId = userDetails.getUserId();
    log.info("Chat session initialization/resume requested for userId: {}", userId);
    // 세션 초기화
    ChatResponse response = chatService.initializeSession(userId);
    log.info("Chat session response sent: sessionId={}, currentStep={}", response.getSessionId(), response.getCurrentStep());

    return ApiResponse.onSuccess(response);
  }

  /**
   * [2. 유저 응답 처리]
   * 유저의 응답(GENDER, BIRTH_YEAR, EMOTION_SELECT 등)을 처리하고
   * 챗봇의 다음 메시지(다음 단계의 질문)를 반환합니다.
   * (온보딩, 감정 대화 모두 이 API 하나로 처리합니다)
   */
  @PostMapping("/session/{sessionId}")
  public ApiResponse<ChatResponse> handleUserResponse(
      @PathVariable String sessionId,
      @Valid @RequestBody ChatRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails)
  {

    Long userId = userDetails.getUserId();
    log.info("User response received. userId: {}, sessionId: {}, response: {}", userId, sessionId, request.getResponseValue());

    ChatResponse response = chatService.handleUserResponse(userId, sessionId, request);

    log.info("Chat response sent. nextStep={}, isCompleted={}", response.getCurrentStep(), response.getIsCompleted());

    return ApiResponse.onSuccess(response);
  }
}
