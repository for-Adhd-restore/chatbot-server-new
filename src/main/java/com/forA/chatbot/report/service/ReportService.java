package com.forA.chatbot.report.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.NotificationHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationLog;
import com.forA.chatbot.medications.repository.MedicationBundleRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;

import com.forA.chatbot.report.dto.ReportResponseDto;
import com.forA.chatbot.user.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  private final UserRepository userRepository;
  private final MedicationBundleRepository medicationBundleRepository;
  private final MedicationLogRepository medicationLogRepository;


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
}
