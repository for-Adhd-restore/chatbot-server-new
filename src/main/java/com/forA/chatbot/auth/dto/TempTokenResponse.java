package com.forA.chatbot.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TempTokenResponse {
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private long expiresIn;
}
