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
