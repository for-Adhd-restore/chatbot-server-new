package com.forA.chatbot.medications.controller;

import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.code.status.SuccessStatus;
import com.forA.chatbot.apiPayload.exception.GeneralException;
import com.forA.chatbot.medications.dto.MedicationLogRequestDto;
import com.forA.chatbot.medications.dto.MedicationLogResponseDto;
import com.forA.chatbot.medications.dto.MedicationRequestDto;
import com.forA.chatbot.medications.dto.MedicationResponseDto;
import com.forA.chatbot.medications.service.MedicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

  private final MedicationService medicationService;

  /**
      * 약 복용 계획 생성
      */
  @PostMapping("/plan")
  public ApiResponse<MedicationResponseDto> createMedicationPlan(
      @Valid @RequestBody MedicationRequestDto requestDto,
      @AuthenticationPrincipal Long userId
  ) {
    log.info("약 복용 계획 생성 요청 - 사용자 ID: {}, 약 이름: {}", userId, requestDto.getName());

    MedicationResponseDto responseDto = medicationService.createMedicationPlan(userId, requestDto);

    log.info("약 복용 계획 생성 완료 - 사용자 ID: {}, 약 이름: {}", userId, responseDto.getName());

    return ApiResponse.of(SuccessStatus.MEDICATION_CREATED, responseDto);
  }

  /**
   * 약 복용 기록 생성
   */
  @PostMapping("/log")
  public ApiResponse<MedicationLogResponseDto> createMedicationLog(
      @Valid @RequestBody MedicationLogRequestDto requestDto,
      @AuthenticationPrincipal Long userId
  ) {
    log.info("약 복용 기록 요청 수신 - 사용자 ID: {}, medicationId: {}", userId, requestDto.getMedicationId());

    MedicationLogResponseDto responseDto = medicationService.createLog(userId, requestDto);

    log.info("약 복용 기록 생성 완료 - historyId: {}", responseDto.getHistoryId());

    return ApiResponse.of(SuccessStatus.MEDICATION_LOG_CREATED, responseDto);
  }
}