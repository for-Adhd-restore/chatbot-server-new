package com.forA.chatbot.medications.dto;

import com.forA.chatbot.medications.validation.ValidNotification;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@ValidNotification
public class NotificationDto {

  @NotNull(message = "알림 활성화 여부는 필수입니다")
  private Boolean isOn;

  private String time;

  @Builder
  public NotificationDto(Boolean isOn, String time) {
    this.isOn = isOn;
    this.time = time;
  }
}
