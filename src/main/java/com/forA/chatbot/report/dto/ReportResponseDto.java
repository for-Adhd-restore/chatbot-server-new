package com.forA.chatbot.report.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

public class ReportResponseDto {

  /**
   * 주간 리포트 응답 DTO
   */
  @Getter
  @Builder
  public static class WeeklyReportResponse {
    private List<DailyMedicationReportDto> weekData;
  }

  /**
   * 월간 리포트 응답 DTO
   */
  @Getter
  @Builder
  public static class MonthlyReportResponse {
    private int year;       // 조회 년도
    private int month;      // 조회 월
    // 일관성을 위해 필드명을 monthData로 변경
    private List<DailyMedicationReportDto> monthData;
  }

  /**
   * 일별 상세 리포트 DTO (주간/월간 공통 사용)
   */
  @Getter
  @Builder
  public static class DailyMedicationReportDto {
    private String date; // "2025-08-18"
    private String dayOfWeek; // "월"
    private List<MedicationStatusDto> medications;
  }

  /**
   * 개별 복용 계획 상태 DTO (주간/월간 공통 사용)
   */
  @Getter
  @Builder
  public static class MedicationStatusDto {
    private Long bundleId;       // 어떤 복용 계획인지 식별자
    private String bundleName;   // 복용 계획 이름 (e.g., "아침 비타민")
    private String scheduledTime;    // 알람 시간 (e.g., "09:00:00")
    private boolean isTaken;     // 복용 여부
  }
}

