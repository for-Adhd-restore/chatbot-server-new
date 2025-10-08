package com.forA.chatbot.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

public class ReportResponseDto {

  @Getter
  @Builder
  public static class WeeklyReportResponse {
    private List<DailyMedicationReportDto> weekData;
  }

  @Getter
  @Builder
  public static class DailyMedicationReportDto {
    private String date; // "2025-08-18"
    private String dayOfWeek; // "월"
    private List<MedicationStatusDto> medications;
  }

  @Getter
  @Builder
  public static class MedicationStatusDto {
    private Long bundleId;       // 어떤 복용 계획인지 식별자
    private String bundleName;   // 복용 계획 이름 (e.g., "아침 비타민")
    private String scheduledTime;    // 복용 시간 (e.g., "09:00:00")
    private boolean isTaken;     // 복용 여부
  }
}

