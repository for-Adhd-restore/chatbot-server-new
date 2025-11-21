/*package com.forA.chatbot.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.repository.ChatMessageRepository;
import com.forA.chatbot.chat.repository.ChatSessionRepository;
import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationLog;
import com.forA.chatbot.medications.repository.MedicationLogRepository;
import com.forA.chatbot.report.dto.ReportResponseDto;
import com.forA.chatbot.user.domain.User;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @Mock
  private ChatSessionRepository chatSessionRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private MedicationLogRepository medicationLogRepository;

  @InjectMocks
  private ReportService reportService;

  private Long userId;
  private LocalDate testDate;

  @BeforeEach
  void setUp() {
    userId = 1L;
    testDate = LocalDate.of(2025, 10, 30);
  }

  @Test
  @DisplayName("주간 감정 리포트 조회 성공 - 정상 케이스")
  void getWeeklyEmotionReport_Success() {
    // given
    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getCurrentWeek()).hasSize(7);
    assertThat(response.getPreviousWeek()).hasSize(7);

    // 데이터가 없는 경우 모두 0.0
    response.getCurrentWeek().forEach(day -> assertThat(day.getEmotionScore()).isEqualTo(0.0));
  }

  @Test
  @DisplayName("챗봇 감정 데이터만 있는 경우 - 긍정 감정")
  void calculateEmotionScore_OnlyChatbotData_Positive() {
    // given
    ChatSession session = createChatSession();
    ChatMessage message = createChatMessage("session1", "EXCITED,JOY"); // 긍정 2개

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 오늘 날짜의 감정 점수 확인 (EXCITED=+1, JOY=+1 -> 평균 1.0)
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("챗봇 감정 데이터만 있는 경우 - 부정 감정")
  void calculateEmotionScore_OnlyChatbotData_Negative() {
    // given
    ChatSession session = createChatSession();
    ChatMessage message = createChatMessage("session1", "ANGER,ANXIETY"); // 부정 2개

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 오늘 날짜의 감정 점수 확인 (ANGER=-1, ANXIETY=-1 -> 평균 -1.0)
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(-1.0);
  }

  @Test
  @DisplayName("챗봇 감정 데이터만 있는 경우 - 혼합 감정")
  void calculateEmotionScore_OnlyChatbotData_Mixed() {
    // given
    ChatSession session = createChatSession();
    ChatMessage message = createChatMessage("session1", "EXCITED,SO_SO,ANGER"); // +1, 0, -1

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 평균 = (1 + 0 + (-1)) / 3 = 0.0
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("복약 감정 데이터만 있는 경우")
  void calculateEmotionScore_OnlyMedicationData() {
    // given
    MedicationLog log1 = createMedicationLog(2); // +2
    MedicationLog log2 = createMedicationLog(-1); // -1

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Arrays.asList(log1, log2));

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 평균 = (2 + (-1)) / 2 = 0.5
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(0.5);
  }

  @Test
  @DisplayName("챗봇 + 복약 감정 데이터가 모두 있는 경우")
  void calculateEmotionScore_BothChatbotAndMedication() {
    // given
    ChatSession session = createChatSession();
    ChatMessage message = createChatMessage("session1", "EXCITED,JOY"); // +1, +1

    MedicationLog log1 = createMedicationLog(2); // +2
    MedicationLog log2 = createMedicationLog(-1); // -1

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Arrays.asList(log1, log2));

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 평균 = (1 + 1 + 2 + (-1)) / 4 = 0.75
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(0.75);
  }

  @Test
  @DisplayName("최대 6개 제한 확인 - 챗봇 3개 + 복약 3개")
  void calculateEmotionScore_MaxLimit() {
    // given
    ChatSession session1 = createChatSession();
    ChatMessage message1 = createChatMessage("session1", "EXCITED"); // 챗봇 1개
    ChatMessage message2 = createChatMessage("session1", "JOY"); // 챗봇 2개
    ChatMessage message3 = createChatMessage("session1", "HAPPY"); // 챗봇 3개
    ChatMessage message4 = createChatMessage("session1", "PROUD"); // 챗봇 4개 (무시됨)

    MedicationLog log1 = createMedicationLog(1);
    MedicationLog log2 = createMedicationLog(1);
    MedicationLog log3 = createMedicationLog(1);
    MedicationLog log4 = createMedicationLog(1); // 4개 (무시됨)

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session1));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message1, message2, message3, message4));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Arrays.asList(log1, log2, log3, log4));

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 최대 6개만 사용: 챗봇 3개(+3) + 복약 3개(+3) = 평균 1.0
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("잘못된 감정 코드는 무시됨")
  void calculateEmotionScore_InvalidEmotionCode() {
    // given
    ChatSession session = createChatSession();
    ChatMessage message = createChatMessage("session1", "EXCITED,INVALID_CODE,JOY");

    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(session));

    when(chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
        anyString(), eq(ChatMessage.SenderType.USER), anyString()))
        .thenReturn(Arrays.asList(message));

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // INVALID_CODE는 무시되고 EXCITED(+1), JOY(+1)만 계산 -> 평균 1.0
    ReportResponseDto.DailyEmotionReportDto todayData =
        response.getCurrentWeek().get(response.getCurrentWeek().size() - 1);
    assertThat(todayData.getEmotionScore()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("요일 형식 확인 - MONDAY 형식")
  void checkDayOfWeekFormat() {
    // given
    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    // 모든 요일이 영어 대문자 형식인지 확인
    response
        .getCurrentWeek()
        .forEach(
            day -> {
              assertThat(day.getDayOfWeek())
                  .matches("^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$");
            });
  }

  @Test
  @DisplayName("날짜 범위 확인 - currentWeek는 오늘 포함 이전 7일")
  void checkDateRange_CurrentWeek() {
    // given
    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    LocalDate today = LocalDate.now();
    LocalDate expectedStart = today.minusDays(6);

    List<ReportResponseDto.DailyEmotionReportDto> currentWeek = response.getCurrentWeek();
    assertThat(currentWeek.get(0).getDate()).isEqualTo(expectedStart.toString());
    assertThat(currentWeek.get(6).getDate()).isEqualTo(today.toString());
  }

  @Test
  @DisplayName("날짜 범위 확인 - previousWeek는 currentWeek 이전 7일")
  void checkDateRange_PreviousWeek() {
    // given
    when(chatSessionRepository.findByUserIdAndCurrentStepAndDateRange(
        anyLong(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());

    when(medicationLogRepository.findByUserIdAndDateAndIsTaken(
        anyLong(), any(Date.class), anyBoolean()))
        .thenReturn(Collections.emptyList());

    // when
    ReportResponseDto.WeeklyEmotionReportResponse response =
        reportService.getWeeklyEmotionReport(userId);

    // then
    LocalDate today = LocalDate.now();
    LocalDate currentWeekStart = today.minusDays(6);
    LocalDate expectedStart = currentWeekStart.minusDays(7);
    LocalDate expectedEnd = currentWeekStart.minusDays(1);

    List<ReportResponseDto.DailyEmotionReportDto> previousWeek = response.getPreviousWeek();
    assertThat(previousWeek.get(0).getDate()).isEqualTo(expectedStart.toString());
    assertThat(previousWeek.get(6).getDate()).isEqualTo(expectedEnd.toString());
  }

  // ========== Helper Methods ==========

  private ChatSession createChatSession() {
    return ChatSession.builder()
        .id("session1")
        .userId(userId)
        .currentStep(ChatStep.EMOTION_SELECT.name())
        .startedAt(LocalDateTime.now())
        .build();
  }

  private ChatMessage createChatMessage(String sessionId, String responseCode) {
    return ChatMessage.builder()
        .id("message1")
        .sessionId(sessionId)
        .senderType(ChatMessage.SenderType.USER)
        .chatStep(ChatStep.EMOTION_SELECT.name())
        .responseCode(responseCode)
        .sentAt(LocalDateTime.now())
        .build();
  }

  private MedicationLog createMedicationLog(Integer medCondition) {
    // 방법 1: ReflectionTestUtils 사용 (User에 Builder가 있는 경우)
    User user = User.builder().build();
    ReflectionTestUtils.setField(user, "id", userId);

    MedicationBundle bundle =
        MedicationBundle.builder()
            .user(user)
            .bundleName("아침 약")
            .alarmEnabled(false)
            .build();
    ReflectionTestUtils.setField(bundle, "id", 1L);

    MedicationLog log =
        MedicationLog.builder()
            .medicationBundle(bundle)
            .date(Date.valueOf(LocalDate.now()))
            .isTaken(true)
            .medCondition(medCondition)
            .build();
    ReflectionTestUtils.setField(log, "id", 1L);

    return log;
  }
}
*/
