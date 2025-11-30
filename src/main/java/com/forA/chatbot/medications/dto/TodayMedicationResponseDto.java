package com.forA.chatbot.medications.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodayMedicationResponseDto {
  private Long medicationId;
  private String name;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime takeTime;

  private List<String> typeTags;

  private List<String> takeDays;

  private NotificationDto notification;

  private TodayHistory todayHistory;

  @Builder
  public TodayMedicationResponseDto(
      Long medicationId,
      String name,
      LocalTime takeTime,
      List<String> typeTags,
      List<String> takeDays,
      NotificationDto notification,
      TodayHistory todayHistory) {
    this.medicationId = medicationId;
    this.name = name;
    this.takeTime = takeTime;
    this.typeTags = typeTags;
    this.takeDays = takeDays;
    this.notification = notification;
    this.todayHistory = todayHistory;
  }

  @Getter
  public static class TodayHistory {
    private String status; // "TAKEN", "SKIPPED", "PENDING"
    private Integer conditionLevel; // -2 ~ +2, nullable

    @Builder
    public TodayHistory(String status, Integer conditionLevel) {
      this.status = status;
      this.conditionLevel = conditionLevel;
    }
  }
}
