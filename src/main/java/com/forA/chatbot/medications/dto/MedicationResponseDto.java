package com.forA.chatbot.medications.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class MedicationResponseDto {

  private Long id;
  private String name;
  private List<String> typeTags;
  private List<String> takeDays;
  private String takeTime;
  private NotificationDto notification;
  private LocalDateTime createdAt;

  @Builder
  public MedicationResponseDto(Long id, String name, List<String> typeTags, List<String> takeDays,
      String takeTime, NotificationDto notification, LocalDateTime createdAt) {
    this.id = id;
    this.name = name;
    this.typeTags = typeTags;
    this.takeDays = takeDays;
    this.takeTime = takeTime;
    this.notification = notification;
    this.createdAt = createdAt;
  }

  // Request에서 Response로 변환하는 정적 메서드
  public static MedicationResponseDto from(MedicationRequestDto request, Long id, LocalDateTime createdAt) {
    return MedicationResponseDto.builder()
        .id(id)
        .name(request.getName())
        .typeTags(request.getTypeTags())
        .takeDays(request.getTakeDays())
        .takeTime(request.getTakeTime())
        .notification(request.getNotification())
        .createdAt(createdAt)
        .build();
  }
}