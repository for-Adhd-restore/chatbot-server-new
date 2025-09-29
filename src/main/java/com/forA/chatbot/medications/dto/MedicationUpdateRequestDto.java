package com.forA.chatbot.medications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MedicationUpdateRequestDto {

  @Size(max = 20, message = "약 이름은 최대 20자까지 입력 가능합니다")
  private String name;

  private List<String> typeTags;

  private List<String> takeDays;

  private String takeTime;

  @Valid private NotificationUpdateDto notification;

  @Builder
  public MedicationUpdateRequestDto(
      String name,
      List<String> typeTags,
      List<String> takeDays,
      String takeTime,
      NotificationUpdateDto notification) {
    this.name = name;
    this.typeTags = typeTags;
    this.takeDays = takeDays;
    this.takeTime = takeTime;
    this.notification = notification;
  }
}
