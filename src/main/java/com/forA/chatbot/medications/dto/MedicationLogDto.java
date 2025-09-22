package com.forA.chatbot.medications.dto;

import com.forA.chatbot.medications.domain.MedicationLog;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MedicationLogDto {
  private Long medicationId;
  private String date;
  private String status;
  private Integer conditionLevel;
  private String takenAt;

  @Builder
  public MedicationLogDto(Long medicationId, String date, String status, Integer conditionLevel, String takenAt) {
    this.medicationId = medicationId;
    this.date = date;
    this.status = status;
    this.conditionLevel = conditionLevel;
    this.takenAt = takenAt;
  }

  /**
   * Entity → DTO 변환 정적 메서드
   */
  public static MedicationLogDto from(MedicationLog entity) {
    return new MedicationLogDto(
        entity.getMedicationBundle().getId(),
        entity.getDate().toString(),
        Boolean.TRUE.equals(entity.getIsTaken()) ? "TAKEN" : "SKIPPED",
        entity.getMedCondition(),
        entity.getTakenAt() != null ? entity.getTakenAt().toString() : null
    );
  }
}