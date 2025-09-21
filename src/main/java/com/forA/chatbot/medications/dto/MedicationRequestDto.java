package com.forA.chatbot.medications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // 파라미터가 없는 디폴트 생성자 생성
public class MedicationRequestDto {

  @NotBlank(message = "약 이름은 필수입니다")
  @Size(max = 20, message = "약 이름은 최대 20자까지 입력 가능합니다")
  private String name;

  @NotEmpty(message = "약 종류 태그는 최소 1개 이상 필요합니다")
  private List<String> typeTags;

  @NotEmpty(message = "복용할 요일은 최소 1개 이상 선택해야 합니다")
  private List<String> takeDays;

  @NotBlank(message = "복용 시간은 필수입니다")
  private String takeTime;

  @Valid
  @NotNull(message = "알림 설정은 필수입니다")
  private NotificationDto notification;

  @Builder
  public MedicationRequestDto(String name, List<String> typeTags, List<String> takeDays, String takeTime, NotificationDto notification) {
    this.name = name;
    this.typeTags = typeTags;
    this.takeDays = takeDays;
    this.takeTime = takeTime;
    this.notification = notification;
  }



}