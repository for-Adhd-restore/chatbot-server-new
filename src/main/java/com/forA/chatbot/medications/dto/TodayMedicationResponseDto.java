package com.forA.chatbot.medications.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Time;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TodayMedicationResponseDto {
  private Long medicationId;
  private String name;

  @JsonFormat(pattern = "HH:mm")
  private Time takeTime;

  private TodayHistory todayHistory;

  @Builder
  public TodayMedicationResponseDto(
      Long medicationId, String name, Time takeTime, TodayHistory todayHistory) {
    this.medicationId = medicationId;
    this.name = name;
    this.takeTime = takeTime;
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
