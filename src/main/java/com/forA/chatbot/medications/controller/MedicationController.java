package com.forA.chatbot.medications.controller;

import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.code.status.SuccessStatus;
import com.forA.chatbot.apiPayload.exception.GeneralException;
import com.forA.chatbot.medications.dto.MedicationRequestDto;
import com.forA.chatbot.medications.dto.MedicationResponseDto;
import com.forA.chatbot.medications.service.MedicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

  private final MedicationService medicationService;
  private final JwtUtil jwtUtil;

  @PostMapping
  public ApiResponse<MedicationResponseDto> createMedicationPlan(
      @Valid @RequestBody MedicationRequestDto requestDto,
      HttpServletRequest request) {

    log.info("약 복용 계획 생성 요청 - 약 이름: {}", requestDto.getName());

    Long userId = jwtUtil.getUserIdFromRequest(request);
    MedicationResponseDto responseDto = medicationService.createMedicationPlan(userId, requestDto);

    log.info("약 복용 계획 생성 완료 - 사용자 ID: {}, 약 이름: {}", userId, responseDto.getName());

    return ApiResponse.of(SuccessStatus.MEDICATION_CREATED, responseDto);
  }
}