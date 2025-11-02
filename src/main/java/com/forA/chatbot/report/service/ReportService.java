package com.forA.chatbot.report.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.NotificationHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.chat.repository.ChatMessageRepository;
import com.forA.chatbot.chat.repository.ChatSessionRepository;
import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationLog;
import com.forA.chatbot.medications.repository.MedicationBundleRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;

import com.forA.chatbot.report.dto.ReportResponseDto;
import com.forA.chatbot.user.domain.User;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  private final UserRepository userRepository;
  private final MedicationBundleRepository medicationBundleRepository;
  private final MedicationLogRepository medicationLogRepository;
  private final ChatSessionRepository chatSessionRepository;
  private final ChatMessageRepository chatMessageRepository;


  public ReportResponseDto.WeeklyReportResponse getWeeklyMedicationReport(Long userId) {
    // 1. 사용자 정보 조회
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotificationHandler(ErrorStatus.USER_NOT_FOUND));

    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(6);

    // 공통 메서드를 호출하여 주간 데이터 생성
    List<ReportResponseDto.DailyMedicationReportDto> weekData = generateDailyReports(user, startDate, today);

    return ReportResponseDto.WeeklyReportResponse.builder()
        .weekData(weekData)
        .build();
  }


  public ReportResponseDto.MonthlyReportResponse getMonthlyMedicationReport(Long userId, int monthOffset) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotificationHandler(ErrorStatus.USER_NOT_FOUND));

    // 1. monthOffset을 기준으로 대상 월의 시작일과 종료일 계산
    YearMonth targetMonth = YearMonth.now().plusMonths(monthOffset);
    LocalDate startDate = targetMonth.atDay(1);
    LocalDate endDate = targetMonth.atEndOfMonth();

    // 2. 공통 로직을 사용하여 일별 리포트 데이터 생성
    List<ReportResponseDto.DailyMedicationReportDto> monthData = generateDailyReports(user, startDate, endDate);

    return ReportResponseDto.MonthlyReportResponse.builder()
        .year(targetMonth.getYear()) // 조회된 년도 추가
        .month(targetMonth.getMonthValue()) // 조회된 월 추가
        .monthData(monthData)
        .build();
  }

  /**
   * 지정된 기간 동안의 일별 복용 리포트 목록을 생성하는 공통 메서드
   * (주간/월간 리포트 생성 로직 중복 제거)
   */
  private List<ReportResponseDto.DailyMedicationReportDto> generateDailyReports(User user, LocalDate startDate, LocalDate endDate) {
    // 사용자의 모든 복용 계획 조회 (알람 시간 순으로 정렬)
    List<MedicationBundle> bundles = medicationBundleRepository.findByUserAndIsDeletedFalseOrderByScheduledTimeAsc(user);

    // 기간 내의 모든 복용 기록 조회 (DB 쿼리 최적화)
    List<MedicationLog> logs = medicationLogRepository.findByMedicationBundle_UserAndDateBetween(user, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));

    // 복용 기록을 날짜와 복용 계획 ID 기준으로 빠르게 찾을 수 있도록 Map으로 변환
    Map<LocalDate, Map<Long, MedicationLog>> logMap = logs.stream()
        .collect(Collectors.groupingBy(
            log -> new java.sql.Date(log.getDate().getTime()).toLocalDate(),
            Collectors.toMap(log -> log.getMedicationBundle().getId(), log -> log)
        ));

    List<ReportResponseDto.DailyMedicationReportDto> dailyReports = new ArrayList<>();
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // 시작일부터 종료일까지 하루씩 반복
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      DayOfWeek dayOfWeekEnum = date.getDayOfWeek();

      // 요일별로 예정된 복용 계획 필터링 (애플리케이션 레벨)
      List<MedicationBundle> scheduledBundles = bundles.stream()
          .filter(bundle -> isScheduledForDay(bundle, dayOfWeekEnum))
          .toList();

      Map<Long, MedicationLog> dailyLogs = logMap.getOrDefault(date, Collections.emptyMap());

      List<ReportResponseDto.MedicationStatusDto> medications = scheduledBundles.stream()
          .map(bundle -> {
            MedicationLog log = dailyLogs.get(bundle.getId());
            boolean isTaken = (log != null && log.getIsTaken());

            return ReportResponseDto.MedicationStatusDto.builder()
                .bundleId(bundle.getId())
                .bundleName(bundle.getBundleName())
                .scheduledTime(bundle.getAlarmTime() != null ? bundle.getAlarmTime().toLocalTime().format(timeFormatter) : null)
                .isTaken(isTaken)
                .build();
          })
          .collect(Collectors.toList());

      dailyReports.add(ReportResponseDto.DailyMedicationReportDto.builder()
          .date(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
          .dayOfWeek(dayOfWeekEnum.getDisplayName(TextStyle.SHORT, Locale.KOREAN))
          .medications(medications)
          .build());
    }
    return dailyReports;
  }

  // MedicationBundle의 dayOfWeek 문자열에 해당 요일이 포함되어 있는지 확인
  private boolean isScheduledForDay(MedicationBundle bundle, DayOfWeek dayOfWeek) {
    if (bundle.getDayOfWeek() == null || bundle.getDayOfWeek().isEmpty()) {
      return false;
    }

    // DayOfWeek enum의 이름(e.g., "MONDAY")을 가져옵니다.
    String englishDayName = dayOfWeek.name();

    // DB에서 가져온 문자열(e.g., "MONDAY,TUESDAY")을 쉼표로 분리하고,
    // 공백 제거 및 대소문자 무시 비교를 통해 현재 요일이 포함되어 있는지 확인합니다.
    return Arrays.stream(bundle.getDayOfWeek().split(","))
        .map(String::trim)
        .anyMatch(dbDay -> dbDay.equalsIgnoreCase(englishDayName));
  }

  /**
   * 주간 감정 리포트 조회
   * - currentWeek: 오늘 포함 이전 7일
   * - previousWeek: currentWeek 이전 7일
   */
  public ReportResponseDto.WeeklyEmotionReportResponse getWeeklyEmotionReport(Long userId) {
    LocalDate today = LocalDate.now();

    // 현재 주 데이터 (오늘 포함 이전 7일)
    LocalDate currentWeekStart = today.minusDays(6);
    LocalDate currentWeekEnd = today;
    List<ReportResponseDto.DailyEmotionReportDto> currentWeekData =
        calculateDailyEmotionScores(userId, currentWeekStart, currentWeekEnd);

    // 이전 주 데이터 (현재 주 이전 7일)
    LocalDate previousWeekStart = currentWeekStart.minusDays(7);
    LocalDate previousWeekEnd = currentWeekStart.minusDays(1);
    List<ReportResponseDto.DailyEmotionReportDto> previousWeekData =
        calculateDailyEmotionScores(userId, previousWeekStart, previousWeekEnd);

    return ReportResponseDto.WeeklyEmotionReportResponse.builder()
        .currentWeek(currentWeekData)
        .previousWeek(previousWeekData)
        .build();
  }

  /**
   * 특정 기간 동안의 일별 감정 점수 계산
   */
  private List<ReportResponseDto.DailyEmotionReportDto> calculateDailyEmotionScores(
      Long userId, LocalDate startDate, LocalDate endDate) {

    List<ReportResponseDto.DailyEmotionReportDto> dailyDataList = new ArrayList<>();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      Double emotionScore = calculateEmotionScoreForDate(userId, date);

      ReportResponseDto.DailyEmotionReportDto dailyData =
          ReportResponseDto.DailyEmotionReportDto.builder()
              .date(date.toString()) // YYYY-MM-DD 형식
              .dayOfWeek(getDayOfWeekInEnglish(date))
              .emotionScore(emotionScore)
              .build();

      dailyDataList.add(dailyData);
    }

    return dailyDataList;
  }

  /**
   * 특정 날짜의 감정 점수 계산
   * - 챗봇 감정 (최대 3번) + 복약 감정 (최대 3번)
   * - 평균 = 전체 점수 합 / 기록 횟수
   * - 기록이 없으면 0.0 반환
   */
  private Double calculateEmotionScoreForDate(Long userId, LocalDate date) {
    List<Integer> emotionScores = new ArrayList<>();

    // 1. 챗봇에서 수집한 감정 점수 계산
    List<Integer> chatbotEmotionScores = getChatbotEmotionScores(userId, date);
    emotionScores.addAll(chatbotEmotionScores);


    // 2. 복약 시 수집한 감정 점수 계산
    List<Integer> medicationEmotionScores = getMedicationEmotionScores(userId, date);
    for(Integer emotionScore : medicationEmotionScores) {
      log.info("복약 감정 점수: {}",String.valueOf(emotionScore));
    }
    emotionScores.addAll(medicationEmotionScores);

    // 3. 평균 계산
    if (emotionScores.isEmpty()) {
      return 0.0;
    }

    int totalScore = emotionScores.stream().mapToInt(Integer::intValue).sum();
    log.info("최종 감정 점수 계산: {} 나누기 {}",String.valueOf(totalScore), String.valueOf(emotionScores.size()));
    return (double) totalScore / emotionScores.size();
  }

  /**
   * 챗봇에서 수집한 감정 점수 조회
   * - ChatSession의 currentStep이 'EMOTION_SELECT'인 세션 찾기
   * - 해당 세션의 ChatMessage에서 SenderType이 USER인 메시지의 responseCode 파싱
   * - EmotionType의 state에 따라 점수 계산: POSITIVE(+1), NEUTRAL(0), NEGATIVE(-1)
   */
  private List<Integer> getChatbotEmotionScores(Long userId, LocalDate date) {
    List<Integer> scores = new ArrayList<>();

    try {
      log.info("챗봇 감정 조회 시작 날짜: {}",String.valueOf(date));
      // 해당 날짜에 EMOTION_SELECT 단계인 ChatSession 조회
      // LocalDate를 LocalDateTime 범위로 변환 (00:00:00 ~ 23:59:59)
      java.time.LocalDateTime startOfDay = date.atStartOfDay();
      java.time.LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

      List<ChatSession> emotionSessions =
          chatSessionRepository.findByUserIdAndStartedAtBetween(
              userId, startOfDay, endOfDay);

      for (ChatSession session : emotionSessions) {
        // 해당 세션의 USER 메시지 중 EMOTION_SELECT 단계의 메시지 조회
        List<ChatMessage> userMessages =
            chatMessageRepository.findBySessionIdAndSenderTypeAndChatStep(
                session.getId(),
                ChatMessage.SenderType.USER,
                ChatStep.EMOTION_SELECT.name());

        for (ChatMessage message : userMessages) {
          String responseCode = message.getResponseCode();
          if (responseCode != null && !responseCode.isEmpty()) {
            // 감정 코드 파싱 (예: "EXCITED,JOY")
            List<Integer> emotionScoresFromMessage = parseEmotionCodes(responseCode);
            for (Integer emotionScore : emotionScoresFromMessage) {
              log.info("챗봇 감정 점수: {}",String.valueOf(emotionScore));
            }

            scores.addAll(emotionScoresFromMessage);
          }
        }
      }

    } catch (Exception e) {
      log.error("챗봇 감정 점수 조회 중 오류 발생: userId={}, date={}", userId, date, e);
    }

    return scores;
  }

  /**
   * 감정 코드 문자열 파싱 및 점수 계산
   * - 예: "EXCITED,JOY" -> [1, 1] (각각 POSITIVE이므로 +1)
   */
  private List<Integer> parseEmotionCodes(String responseCode) {
    List<Integer> scores = new ArrayList<>();

    String[] emotionCodes = responseCode.split(",");
    for (String code : emotionCodes) {
      try {
        EmotionType emotionType = EmotionType.valueOf(code.trim());
        int score = getEmotionScore(emotionType);
        scores.add(score);
      } catch (IllegalArgumentException e) {
        log.warn("알 수 없는 감정 코드: {}", code);
      }
    }

    return scores;
  }

  /**
   * EmotionType의 state에 따라 점수 반환
   * - POSITIVE: +1
   * - NEUTRAL: 0
   * - NEGATIVE: -1
   */
  private int getEmotionScore(EmotionType emotionType) {
    switch (emotionType.getState()) {
      case POSITIVE:
        return 1;
      case NEUTRAL:
        return 0;
      case NEGATIVE:
        return -1;
      default:
        return 0;
    }
  }

  /**
   * 복약 시 수집한 감정 점수 조회
   * - MedicationLog의 medCondition 값을 그대로 사용
   */
  private List<Integer> getMedicationEmotionScores(Long userId, LocalDate date) {
    List<Integer> scores = new ArrayList<>();

    try {
      Date sqlDate = Date.valueOf(date);
      List<MedicationLog> medicationLogs =
          medicationLogRepository.findByUserIdAndDateAndIsTaken(userId, sqlDate, true);

      for (MedicationLog log : medicationLogs) {
        if (log.getMedCondition() != null) {
          scores.add(log.getMedCondition());
        }
      }

      // 최대 3번까지만 사용
      if (scores.size() > 3) {
        return scores.subList(0, 3);
      }

    } catch (Exception e) {
      log.error("복약 감정 점수 조회 중 오류 발생: userId={}, date={}", userId, date, e);
    }

    return scores;
  }

  /**
   * 요일을 영어 대문자 풀네임으로 반환
   * - 예: MONDAY, TUESDAY, WEDNESDAY, ...
   */
  private String getDayOfWeekInEnglish(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();
  }


}
