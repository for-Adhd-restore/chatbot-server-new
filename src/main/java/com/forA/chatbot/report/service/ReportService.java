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
import com.forA.chatbot.report.dto.ReportResponseDto.MonthlyEmotionReportResponse;
import com.forA.chatbot.report.dto.ReportResponseDto.WeeklyReportSummaryResponse;
import com.forA.chatbot.user.domain.User;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
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

  //특정 날짜의 감정 점수 계산
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


   // 챗봇에서 수집한 감정 점수 조회
  private List<Integer> getChatbotEmotionScores(Long userId, LocalDate date) {
    List<Integer> scores = new ArrayList<>();

    try {
      log.info("챗봇 감정 조회 시작 날짜: {}",String.valueOf(date));
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


  public MonthlyEmotionReportResponse getMonthlyEmotionReport(Long userId, int monthOffset) {
    // 1. monthOffset을 기준으로 대상 월의 시작일과 종료일 계산
    YearMonth targetMonth = YearMonth.now().plusMonths(monthOffset);
    LocalDate startDate = targetMonth.atDay(1);
    LocalDate endDate = targetMonth.atEndOfMonth();

    // 2. 공통 로직을 사용하여 일별 리포트 데이터 생성
    List<ReportResponseDto.DailyEmotionReportDto> monthData = calculateDailyEmotionScores(userId, startDate, endDate);

    return ReportResponseDto.MonthlyEmotionReportResponse.builder()
        .year(targetMonth.getYear()) // 조회된 년도 추가
        .month(targetMonth.getMonthValue()) // 조회된 월 추가
        .monthData(monthData)
        .build();

  }

  public ReportResponseDto.WeeklyReportSummaryResponse getWeeklyReportSummary(Long userId) {
    // 1. 사용자 정보 조회
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotificationHandler(ErrorStatus.USER_NOT_FOUND));


    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(6);

    // 감정 점수 변화율
    Double emotionRate = calculateEmotionScoreChangeRate(userId, user.getCreatedAt().toLocalDate(), today);
    // 저번주 복약 비율
    Double medicationRate = calculateLastWeekMedicationRate(userId, user.getCreatedAt().toLocalDate(), today);


    return WeeklyReportSummaryResponse.builder()
        .hasEmotionData(emotionRate != null)
        .hasMedicationData(medicationRate != null)
        .emotionImprovementRate(emotionRate)  // null이면 JSON에서 제외됨
        .medicationComplianceRate(medicationRate)  // null이면 JSON에서 제외됨
        .build();

  }

  /**
   * 저번주 복약 비율 계산
   */
  private Double calculateLastWeekMedicationRate(Long userId, LocalDate joinDate, LocalDate today) {
    // 저번주 기간 계산 (월요일~일요일)
    LocalDate lastWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
    LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

    // 신규 사용자 체크: 가입 후 1주일 데이터 필요
    LocalDate minimumDataDate = getMinimumDataDateForMedication(joinDate);
    if (today.isBefore(minimumDataDate)) {
      log.debug("복약 데이터 부족 - userId: {}, 최소 요구일: {}", userId, minimumDataDate);
      return null;
    }

    // 사용자의 활성화된 MedicationBundle 목록 조회
    List<MedicationBundle> bundles = medicationBundleRepository
        .findByUserIdAndIsDeletedFalse(userId);

    if (bundles.isEmpty()) {
      log.debug("복약 계획 없음 - userId: {}", userId);
      return null;
    }

    // 저번주에 먹어야 했던 총 횟수 계산
    int expectedCount = calculateExpectedMedicationCount(bundles, lastWeekStart, lastWeekEnd);

    if (expectedCount == 0) {
      log.debug("저번주 복약 예정 없음 - userId: {}", userId);
      return null;
    }

    // 실제로 복용한 횟수 계산
    Long actualCount = medicationLogRepository.countByBundleIdsAndDateRangeAndIsTaken(
        bundles.stream().map(MedicationBundle::getId).collect(Collectors.toList()),
        Date.valueOf(lastWeekStart),
        Date.valueOf(lastWeekEnd),
        true
    );

    // 복약 비율 계산: (실제 복용 / 예상 복용) * 100
    double rate = ((double) actualCount / expectedCount) * 100;
    return Math.round(rate * 100.0) / 100.0; // 소수점 2자리
  }

  /**
   * 감정 점수 변화율 계산
   * 저번주 vs 그 전주(2주전) 비교
   */
  /**
   * 감정 점수 변화율 계산
   * 저번주 vs 그 전주(2주전) 비교
   */
  private Double calculateEmotionScoreChangeRate(Long userId, LocalDate joinDate, LocalDate today) {
    // 저번주 기간 계산 (월요일~일요일)
    LocalDate lastWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
    LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

    // 그 전주 기간 계산
    LocalDate twoWeeksAgoStart = lastWeekStart.minusWeeks(1);
    LocalDate twoWeeksAgoEnd = twoWeeksAgoStart.plusDays(6);

    // 신규 사용자 체크: 가입 후 2주 데이터 필요
    LocalDate minimumDataDate = getMinimumDataDateForEmotion(joinDate);
    if (today.isBefore(minimumDataDate)) {
      log.debug("감정 데이터 부족 - userId: {}, 최소 요구일: {}", userId, minimumDataDate);
      return null;
    }

    // 저번주 감정 점수 평균
    Double lastWeekAvg = calculateEmotionScoreAverage(
        userId,
        lastWeekStart.atStartOfDay(),
        lastWeekEnd.atTime(23, 59, 59)
    );

    // 그 전주 감정 점수 평균
    Double twoWeeksAgoAvg = calculateEmotionScoreAverage(
        userId,
        twoWeeksAgoStart.atStartOfDay(),
        twoWeeksAgoEnd.atTime(23, 59, 59)
    );

    // 데이터가 없으면 null 반환
    if (lastWeekAvg == null || twoWeeksAgoAvg == null || twoWeeksAgoAvg == 0) {
      log.debug("감정 데이터 계산 불가 - userId: {}, lastWeek: {}, twoWeeksAgo: {}",
          userId, lastWeekAvg, twoWeeksAgoAvg);
      return null;
    }

    // 변화율 계산: (Scurr - Sprev) / |Sprev| * 100
    double changeRate = ((lastWeekAvg - twoWeeksAgoAvg) / Math.abs(twoWeeksAgoAvg)) * 100;
    return Math.round(changeRate * 100.0) / 100.0; // 소수점 2자리
  }

  private LocalDate getMinimumDataDateForMedication(LocalDate joinDate) {
    // 가입일이 속한 주의 일요일 찾기
    LocalDate firstWeekSunday = joinDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

    // 다음주 월요일
    return firstWeekSunday.plusDays(1);
  }

  /**
   * 저번주에 먹어야 하는 총 횟수 계산
   * dayOfWeek 필드를 파싱하여 저번주에 해당하는 요일 개수를 합산
   */
  private int calculateExpectedMedicationCount(
      List<MedicationBundle> bundles,
      LocalDate weekStart,
      LocalDate weekEnd) {

    int totalCount = 0;

    for (MedicationBundle bundle : bundles) {
      String dayOfWeek = bundle.getDayOfWeek();
      if (dayOfWeek == null || dayOfWeek.isEmpty()) {
        continue;
      }

      // dayOfWeek 파싱 (예: "MON,WED,FRI")
      Set<DayOfWeek> scheduledDays = parseDayOfWeek(dayOfWeek);

      // 저번주(월~일) 동안 해당 요일이 몇 번 나오는지 계산
      LocalDate currentDate = weekStart;
      while (!currentDate.isAfter(weekEnd)) {
        if (scheduledDays.contains(currentDate.getDayOfWeek())) {
          totalCount++;
        }
        currentDate = currentDate.plusDays(1);
      }
    }

    return totalCount;
  }

  /**
   * dayOfWeek 문자열을 DayOfWeek Set으로 변환
   * 예: "MONDAY,WEDNESDAY,FRIDAY" -> {MONDAY, WEDNESDAY, FRIDAY}
   */
  private Set<DayOfWeek> parseDayOfWeek(String dayOfWeek) {
    return Arrays.stream(dayOfWeek.split(","))
        .map(String::trim)
        .map(DayOfWeek::valueOf) // "MONDAY" -> DayOfWeek.MONDAY
        .collect(Collectors.toSet());
  }

  /**
   * 특정 기간 감정 점수 평균 계산
   * 기간 내 각 날짜의 감정 점수를 계산하고 전체 평균을 반환
   */
  private Double calculateEmotionScoreAverage(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
    LocalDate startLocalDate = startDate.toLocalDate();
    LocalDate endLocalDate = endDate.toLocalDate();

    List<Double> dailyScores = new ArrayList<>();

    // 기간 내 모든 날짜를 순회하며 감정 점수 계산
    LocalDate currentDate = startLocalDate;
    while (!currentDate.isAfter(endLocalDate)) {
      Double dailyScore = calculateEmotionScoreForDate(userId, currentDate);

      // 해당 날짜에 데이터가 있는 경우만 포함 (0.0이 아닌 경우)
      if (dailyScore != null && dailyScore > 0.0) {
        dailyScores.add(dailyScore);
      }

      currentDate = currentDate.plusDays(1);
    }

    // 데이터가 없으면 null 반환
    if (dailyScores.isEmpty()) {
      return null;
    }

    // 전체 기간 평균 계산
    double average = dailyScores.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);

    return Math.round(average * 100.0) / 100.0; // 소수점 2자리
  }

  /**
   * 감정 데이터를 볼 수 있는 최소 날짜 계산
   * 예: 8월 19일(화) 가입 → 9월 1일(월)부터 변화율 확인 가능
   */
  private LocalDate getMinimumDataDateForEmotion(LocalDate joinDate) {
    // 가입일이 속한 주의 월요일 찾기
    LocalDate firstWeekMonday = joinDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    // 2주차 데이터 필요 → 3주차 월요일부터 확인 가능
    return firstWeekMonday.plusWeeks(2);
  }
}
