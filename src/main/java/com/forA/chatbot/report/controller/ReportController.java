package com.forA.chatbot.report.controller;

import com.forA.chatbot.apiPayload.ApiResponse;
import com.forA.chatbot.apiPayload.code.status.SuccessStatus;
import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.report.dto.ReportResponseDto;
import com.forA.chatbot.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

  private final ReportService reportService;

  @GetMapping("/weekly/medication")
  public ApiResponse<ReportResponseDto.WeeklyReportResponse> getWeeklyMedicationReport(
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    Long userId = userDetails.getUserId();

    ReportResponseDto.WeeklyReportResponse response = reportService.getWeeklyMedicationReport(userId);

    return ApiResponse.of(SuccessStatus._WEEKLY_MEDICATION_REPORT_RETRIEVED,response);
  }
}
