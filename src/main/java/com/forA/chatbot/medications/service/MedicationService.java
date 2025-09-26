package com.forA.chatbot.medications.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.GeneralException;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationItem;
import com.forA.chatbot.medications.domain.MedicationLog;
import com.forA.chatbot.medications.dto.MedicationLogRequestDto;
import com.forA.chatbot.medications.dto.MedicationLogResponseDto;
import com.forA.chatbot.medications.dto.MedicationRequestDto;
import com.forA.chatbot.medications.dto.MedicationResponseDto;
import com.forA.chatbot.medications.dto.NotificationDto;
import com.forA.chatbot.medications.dto.TodayMedicationResponseDto;
import com.forA.chatbot.medications.repository.MedicationBundleRepository;
import com.forA.chatbot.medications.repository.MedicationItemRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;
import com.forA.chatbot.user.User;
import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MedicationService {

  private final MedicationBundleRepository medicationBundleRepository;
  private final MedicationItemRepository medicationItemRepository;
  private final UserRepository userRepository;
  private final MedicationLogRepository medicationLogRepository;

  public MedicationResponseDto createMedicationPlan(Long userId, MedicationRequestDto requestDto) {
    log.info("약 복용 계획 생성 시작 - 사용자 ID: {}, 약 이름: {}", userId, requestDto.getName());

    // 1. 사용자 조회
    User user = findUserById(userId);

    // 2. 시간 파싱
    Time scheduledTime = parseTime(requestDto.getTakeTime());
    Time alarmTime = parseAlarmTime(requestDto.getNotification());

    // 3. 요일 문자열 생성
    String dayOfWeekStr = String.join(",", requestDto.getTakeDays());

    // 4. MedicationBundle 생성 및 저장
    MedicationBundle medicationBundle =
        MedicationBundle.builder()
            .user(user)
            .bundleName(requestDto.getName())
            .dayOfWeek(dayOfWeekStr)
            .scheduledTime(scheduledTime)
            .alarmEnabled(requestDto.getNotification().getIsOn())
            .alarmTime(alarmTime)
            .build();

    MedicationBundle savedMedicationBundle = medicationBundleRepository.save(medicationBundle);
    log.info("MedicationBundle 저장 완료 - ID: {}", savedMedicationBundle.getId());

    // 5. MedicationItem들 생성 및 저장
    List<MedicationItem> medicationItems =
        requestDto.getTypeTags().stream()
            .map(
                typeTag ->
                    MedicationItem.builder()
                        .medicationBundle(savedMedicationBundle)
                        .medicationName(typeTag)
                        .build())
            .collect(Collectors.toList());

    medicationItemRepository.saveAll(medicationItems);
    log.info("MedicationItem {} 개 저장 완료", medicationItems.size());

    // 6. 응답 DTO 생성
    MedicationResponseDto responseDto =
        MedicationResponseDto.from(requestDto, savedMedicationBundle.getId(), LocalDateTime.now());

    log.info("약 복용 계획 생성 완료 - ID: {}", savedMedicationBundle.getId());
    return responseDto;
  }

  public MedicationLogResponseDto createLog(Long userId, MedicationLogRequestDto requestDto) {
    log.info("약 복용 기록 생성 시작 - 사용자 ID: {}, medicationId: {}", userId, requestDto.getMedicationId());

    // 1. 약 번들 조회
    MedicationBundle bundle =
        medicationBundleRepository
            .findById(requestDto.getMedicationId())
            .orElseThrow(
                () -> {
                  log.error("MedicationBundle을 찾을 수 없음 - ID: {}", requestDto.getMedicationId());
                  return new GeneralException(ErrorStatus.MEDICATION_PLAN_NOT_FOUND);
                });

    // 2. 상태 검증
    boolean isTaken = "TAKEN".equalsIgnoreCase(requestDto.getStatus());
    if (isTaken && requestDto.getConditionLevel() == null) {
      throw new GeneralException(ErrorStatus.MEDICATION_CONDITION_REQUIRED);
    }

    // 3. 날짜 변환
    Date sqlDate = parseDate(requestDto.getDate());

    // 4. 시간 변환 (nullable)
    Time takenTime = parseTime(requestDto.getTakenAt());

    // 5. MedicationLog 생성 및 저장
    MedicationLog logEntity =
        MedicationLog.builder()
            .medicationBundle(bundle)
            .date(sqlDate)
            .isTaken(isTaken)
            .takenAt(takenTime)
            .medCondition(requestDto.getConditionLevel())
            .build();

    MedicationLog saved = medicationLogRepository.save(logEntity);
    log.info("MedicationLog 저장 완료 - historyId: {}", saved.getId());

    // 6. Response 변환
    return MedicationLogResponseDto.from(saved);
  }

  public List<TodayMedicationResponseDto> getTodayMedications(Long userId) {
    log.info("오늘의 복약 계획 조회 요청 - 사용자 ID: {}", userId);

    // 오늘 요일 계산 (영어)
    DayOfWeek today = LocalDate.now().getDayOfWeek();
    String todayDayOfWeek = today.name(); // "MONDAY", "TUESDAY" 등
    log.info("오늘 요일: "+todayDayOfWeek);
    // 오늘 날짜
    Date todayDate = Date.valueOf(LocalDate.now());

    // 오늘 요일에 해당하는 복약 계획들 조회
    List<MedicationBundle> todayBundles = medicationBundleRepository
        .findByUserIdAndDayOfWeek(userId, todayDayOfWeek);

    log.info("조회된 복약 계획 수: {}", todayBundles.size());

    // 각 복약 계획에 대해 오늘의 기록 조회 및 응답 생성
    List<TodayMedicationResponseDto> responses = todayBundles.stream()
        .map(bundle -> {
          Optional<MedicationLog> todayLog = medicationLogRepository
              .findByMedicationBundleAndDate(bundle, todayDate);

          TodayMedicationResponseDto.TodayHistory todayHistory = createTodayHistory(todayLog);

          return TodayMedicationResponseDto.builder()
              .medicationId(bundle.getId())
              .name(bundle.getBundleName())
              .takeTime(bundle.getScheduledTime())
              .todayHistory(todayHistory)
              .build();
        })
        .collect(Collectors.toList());

    log.info("오늘의 복약 계획 조회 완료 - 사용자 ID: {}, 계획 수: {}", userId, responses.size());

    return responses;
  }

  private TodayMedicationResponseDto.TodayHistory createTodayHistory(Optional<MedicationLog> logOpt) {
    if (logOpt.isEmpty()) {
      // 기록이 없으면 PENDING 상태
      return TodayMedicationResponseDto.TodayHistory.builder()
          .status("PENDING")
          .conditionLevel(null)
          .build();
    }

    MedicationLog log = logOpt.get();
    String status = determineStatus(log);

    return TodayMedicationResponseDto.TodayHistory.builder()
        .status(status)
        .conditionLevel(log.getMedCondition())
        .build();
  }

  private String determineStatus(MedicationLog log) {
    if (log.getIsTaken()) {
      return "TAKEN";
    } else {
      return "SKIPPED";
    }
  }

  private User findUserById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.error("사용자를 찾을 수 없습니다 - ID: {}", userId);
              return new GeneralException(ErrorStatus.MEDICATION_USER_NOT_FOUND);
            });
  }

  private Time parseAlarmTime(NotificationDto notification) {
    if (Boolean.TRUE.equals(notification.getIsOn())
        && notification.getTime() != null
        && !notification.getTime().trim().isEmpty()) {
      return parseTime(notification.getTime());
    }
    return null;
  }

  private Time parseTime(String timeStr) {
    try {
      LocalTime localTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
      return Time.valueOf(localTime);
    } catch (DateTimeParseException e) {
      log.error("시간 파싱 실패 - 입력값: {}", timeStr, e);
      throw new GeneralException(ErrorStatus.MEDICATION_INVALID_TIME_FORMAT);
    }
  }

  private Date parseDate(String dateStr) {
    try {
      return Date.valueOf(LocalDate.parse(dateStr));
    } catch (DateTimeParseException e) {
      log.error("날짜 파싱 실패 - 입력값: {}", dateStr, e);
      throw new GeneralException(ErrorStatus.MEDICATION_INVALID_DATE_FORMAT);
    }
  }
}
