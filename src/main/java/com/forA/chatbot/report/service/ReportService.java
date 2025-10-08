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

    // 2. 조회 기간 설정 (오늘 포함 지난 7일)
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(6);

    // 3. 사용자의 모든 복용 계획 조회 (복용 시간 순으로 정렬)
    List<MedicationBundle> bundles = medicationBundleRepository.findByUserAndIsDeletedFalseOrderByScheduledTimeAsc(user);

    // 4. 기간 내의 모든 복용 기록 조회
    List<MedicationLog> logs = medicationLogRepository.findByMedicationBundle_UserAndDateBetween(user, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(today));

    // 5. 복용 기록을 날짜와 복용 계획 ID 기준으로 빠르게 찾을 수 있도록 Map으로 변환
    Map<LocalDate, Map<Long, MedicationLog>> logMap = logs.stream()
        .collect(Collectors.groupingBy(
            log -> new java.sql.Date(log.getDate().getTime()).toLocalDate(),
            Collectors.toMap(log -> log.getMedicationBundle().getId(), log -> log)
        ));

    // 6. 주간 데이터 생성
    List<ReportResponseDto.DailyMedicationReportDto> weekData = new ArrayList<>();
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    for (int i = 0; i < 7; i++) {
      LocalDate currentDate = startDate.plusDays(i);
      DayOfWeek dayOfWeekEnum = currentDate.getDayOfWeek();

      // 요일별로 예정된 복용 계획 필터링
      List<MedicationBundle> scheduledBundles = bundles.stream()
          .filter(bundle -> isScheduledForDay(bundle, dayOfWeekEnum))
          .toList();

      // 해당 날짜의 복용 기록 조회
      Map<Long, MedicationLog> dailyLogs = logMap.getOrDefault(currentDate, Collections.emptyMap());

      // 복용 상태(MedicationStatusDto) 리스트 생성
      List<ReportResponseDto.MedicationStatusDto> medications = scheduledBundles.stream()
          .map(bundle -> {
            MedicationLog log = dailyLogs.get(bundle.getId());
            boolean isTaken = (log != null && log.getIsTaken());

            return ReportResponseDto.MedicationStatusDto.builder()
                .bundleId(bundle.getId())
                .bundleName(bundle.getBundleName())
                .scheduledTime(bundle.getScheduledTime() != null ? bundle.getScheduledTime().toLocalTime().format(timeFormatter) : null)
                .isTaken(isTaken)
                .build();
          })
          .collect(Collectors.toList());

      weekData.add(ReportResponseDto.DailyMedicationReportDto.builder()
          .date(currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
          .dayOfWeek(dayOfWeekEnum.getDisplayName(TextStyle.SHORT, Locale.KOREAN))
          .medications(medications)
          .build());
    }

    return ReportResponseDto.WeeklyReportResponse.builder()
        .weekData(weekData)
        .build();
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
