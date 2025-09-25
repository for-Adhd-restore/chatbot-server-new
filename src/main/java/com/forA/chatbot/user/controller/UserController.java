package com.forA.chatbot.user.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.user.dto.NicknameRequest;
import com.forA.chatbot.user.dto.NicknameResponse;
import com.forA.chatbot.user.dto.UserProfileResponse;
import com.forA.chatbot.user.dto.UserProfileUpdateRequest;
import com.forA.chatbot.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
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
  public ApiResponse<NicknameResponse> createNickname(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody NicknameRequest request
  ) {
    Long userId = userDetails.getUserId();
    NicknameResponse response = userService.updateNickname(userId, request.getNickname());
    
    return ApiResponse.onSuccess(response);
  }

  @PatchMapping("/me")
  public ApiResponse<UserProfileResponse> updateUserProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody UserProfileUpdateRequest request
  ) {
    Long userId = userDetails.getUserId();
    UserProfileResponse response = userService.updateUserProfile(userId, request);
    
    return ApiResponse.onSuccess(response);
  }

  /*TODO : 회원 탈퇴*/

  /*TODO: 사용자 정보 조회*/


  /*TODO: 회원 정보 초기화*/

}
