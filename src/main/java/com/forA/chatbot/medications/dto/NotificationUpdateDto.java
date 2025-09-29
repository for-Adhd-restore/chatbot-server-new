package com.forA.chatbot.medications.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationUpdateDto {

  private Boolean isOn;
  private String time;

  @Builder
  public NotificationUpdateDto(Boolean isOn, String time) {
    this.isOn = isOn;
    this.time = time;
  }
}
