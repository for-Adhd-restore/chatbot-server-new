package com.forA.chatbot.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDeleteResponse {

  private String message;

  public static UserDeleteResponse success() {
    return new UserDeleteResponse(
        "Your account has been deactivated. It will be permanently deleted in 30 days.");
  }
}
