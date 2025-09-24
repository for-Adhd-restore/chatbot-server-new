package com.forA.chatbot.user.controller;


import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;

  @PostMapping("/nickname")
  public ApiResponse<?> createNickname(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestBody String nickname) {
    Long userId = userDetails.getUserId();

  }
}
