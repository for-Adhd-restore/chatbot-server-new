/*package com.forA.chatbot.report.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.forA.chatbot.auth.jwt.CustomUserDetails;
import com.forA.chatbot.auth.jwt.JwtAuthenticationFilter;
import com.forA.chatbot.auth.jwt.JwtUtil;
import com.forA.chatbot.report.dto.ReportResponseDto;
import com.forA.chatbot.report.service.ReportService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class ReportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean
  private ReportService reportService;


  @Test
  @DisplayName("주간 감정 리포트 조회 API 성공")
  void getWeeklyEmotionReport_Success() throws Exception {
    // given
    LocalDate today = LocalDate.now();
    List<ReportResponseDto.DailyEmotionReportDto> currentWeek = createMockWeekData(today, -6);
    List<ReportResponseDto.DailyEmotionReportDto> previousWeek = createMockWeekData(today, -13);

    ReportResponseDto.WeeklyEmotionReportResponse mockResponse =
        ReportResponseDto.WeeklyEmotionReportResponse.builder()
            .currentWeek(currentWeek)
            .previousWeek(previousWeek)
            .build();

    when(reportService.getWeeklyEmotionReport(anyLong())).thenReturn(mockResponse);

    // CustomUserDetails 생성 (userId = 1L)
    CustomUserDetails customUserDetails = new CustomUserDetails(1L);

    // when & then
    mockMvc
        .perform(
            get("/api/v1/reports/weekly/emotion")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .with(user(customUserDetails))) // CustomUserDetails 주입
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.code").value("REPORT2002"))
        .andExpect(jsonPath("$.message").value("주간 감정 리포트를 성공적으로 조회했습니다."))
        .andExpect(jsonPath("$.result.currentWeek").isArray())
        .andExpect(jsonPath("$.result.currentWeek.length()").value(7))
        .andExpect(jsonPath("$.result.previousWeek").isArray())
        .andExpect(jsonPath("$.result.previousWeek.length()").value(7))
        .andExpect(jsonPath("$.result.currentWeek[0].date").exists())
        .andExpect(jsonPath("$.result.currentWeek[0].dayOfWeek").exists())
        .andExpect(jsonPath("$.result.currentWeek[0].emotionScore").exists());
  }

  @Test
  @DisplayName("주간 감정 리포트 조회 - 데이터 값 검증")
  @WithMockUser(username = "1")
  void getWeeklyEmotionReport_ValidateData() throws Exception {
    // given
    LocalDate testDate = LocalDate.of(2025, 10, 30);

    List<ReportResponseDto.DailyEmotionReportDto> currentWeek =
        Arrays.asList(
            createDailyData("2025-10-24", "FRIDAY", 1.5),
            createDailyData("2025-10-25", "SATURDAY", 0.0),
            createDailyData("2025-10-26", "SUNDAY", -0.5),
            createDailyData("2025-10-27", "MONDAY", 1.0),
            createDailyData("2025-10-28", "TUESDAY", 0.75),
            createDailyData("2025-10-29", "WEDNESDAY", -1.0),
            createDailyData("2025-10-30", "THURSDAY", 2.0));

    List<ReportResponseDto.DailyEmotionReportDto> previousWeek =
        Arrays.asList(
            createDailyData("2025-10-17", "FRIDAY", 0.5),
            createDailyData("2025-10-18", "SATURDAY", 0.0),
            createDailyData("2025-10-19", "SUNDAY", 0.0),
            createDailyData("2025-10-20", "MONDAY", 1.0),
            createDailyData("2025-10-21", "TUESDAY", 0.0),
            createDailyData("2025-10-22", "WEDNESDAY", -0.33),
            createDailyData("2025-10-23", "THURSDAY", 0.67));

    ReportResponseDto.WeeklyEmotionReportResponse mockResponse =
        ReportResponseDto.WeeklyEmotionReportResponse.builder()
            .currentWeek(currentWeek)
            .previousWeek(previousWeek)
            .build();

    when(reportService.getWeeklyEmotionReport(anyLong())).thenReturn(mockResponse);

    // CustomUserDetails 생성 (userId = 1L)
    CustomUserDetails customUserDetails = new CustomUserDetails(1L);

    // when & then
    mockMvc
        .perform(
            get("/api/v1/reports/weekly/emotion")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .with(user(customUserDetails)))
        .andDo(print())
        .andExpect(status().isOk())
        // currentWeek 첫 번째 날짜 검증
        .andExpect(jsonPath("$.result.currentWeek[0].date").value("2025-10-24"))
        .andExpect(jsonPath("$.result.currentWeek[0].dayOfWeek").value("FRIDAY"))
        .andExpect(jsonPath("$.result.currentWeek[0].emotionScore").value(1.5))
        // currentWeek 마지막 날짜 검증
        .andExpect(jsonPath("$.result.currentWeek[6].date").value("2025-10-30"))
        .andExpect(jsonPath("$.result.currentWeek[6].dayOfWeek").value("THURSDAY"))
        .andExpect(jsonPath("$.result.currentWeek[6].emotionScore").value(2.0))
        // previousWeek 검증
        .andExpect(jsonPath("$.result.previousWeek[0].date").value("2025-10-17"))
        .andExpect(jsonPath("$.result.previousWeek[0].emotionScore").value(0.5));
  }


  // ========== Helper Methods ==========

  private List<ReportResponseDto.DailyEmotionReportDto> createMockWeekData(
      LocalDate baseDate, int startDayOffset) {
    return Arrays.asList(
        createDailyData(baseDate.plusDays(startDayOffset).toString(), "MONDAY", 0.0),
        createDailyData(baseDate.plusDays(startDayOffset + 1).toString(), "TUESDAY", 1.0),
        createDailyData(baseDate.plusDays(startDayOffset + 2).toString(), "WEDNESDAY", -0.5),
        createDailyData(baseDate.plusDays(startDayOffset + 3).toString(), "THURSDAY", 0.5),
        createDailyData(baseDate.plusDays(startDayOffset + 4).toString(), "FRIDAY", 1.5),
        createDailyData(baseDate.plusDays(startDayOffset + 5).toString(), "SATURDAY", 0.0),
        createDailyData(baseDate.plusDays(startDayOffset + 6).toString(), "SUNDAY", -1.0));
  }

  private ReportResponseDto.DailyEmotionReportDto createDailyData(
      String date, String dayOfWeek, Double emotionScore) {
    return ReportResponseDto.DailyEmotionReportDto.builder()
        .date(date)
        .dayOfWeek(dayOfWeek)
        .emotionScore(emotionScore)
        .build();
  }
}
*/
