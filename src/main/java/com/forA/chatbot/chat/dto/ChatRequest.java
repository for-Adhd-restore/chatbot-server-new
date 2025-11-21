package com.forA.chatbot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {
  // 유저 응답 값 (텍스트 입력 또는 버튼 선택의 코드/값)
  @NotBlank(message = "응답 값은 필수입니다.")
  private String responseValue;
}
