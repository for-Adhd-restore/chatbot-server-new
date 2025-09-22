package com.forA.chatbot.medications.dto;

import com.forA.chatbot.medications.domain.MedicationLog;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MedicationLogResponseDto {

  private Long historyId;
  private String message;
  private MedicationLogDto medicationLog;

  /**
   * 빌더는 응답 생성 시 필요한 필드 전용
   */
  @Builder
  public MedicationLogResponseDto(Long historyId, String message, MedicationLogDto medicationLog) {
    this.historyId = historyId;
    this.message = message;
    this.medicationLog = medicationLog;
  }

  /**
   * Entity → Response 변환 정적 메서드
   */
  public static MedicationLogResponseDto from(MedicationLog entity) {
    return MedicationLogResponseDto.builder()
        .historyId(entity.getId())
        .message("Medication history successfully recorded.")
        .medicationLog(MedicationLogDto.from(entity))
        .build();
  }
}
