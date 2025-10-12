package com.forA.chatbot.user.service;

import com.forA.chatbot.auth.repository.RefreshTokenRepository;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.auth.service.BlacklistService;
import com.forA.chatbot.medications.repository.MedicationBundleRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;
import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import com.forA.chatbot.user.dto.NicknameResponse;
import com.forA.chatbot.user.dto.UserDeleteResponse;
import com.forA.chatbot.user.dto.UserProfileResponse;
import com.forA.chatbot.user.dto.UserProfileUpdateRequest;
import com.forA.chatbot.user.dto.UserResetResponse;
import com.forA.chatbot.user.util.EnumConverterUtil;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final MedicationLogRepository medicationLogRepository;
  private final MedicationBundleRepository medicationBundleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final BlacklistService blacklistService;

  @Transactional
  public NicknameResponse updateNickname(Long userId, String nickname) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    user.updateNickname(nickname);
    userRepository.save(user);

    return new NicknameResponse(nickname);
  }

  @Transactional
  public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 닉네임 업데이트
    if (request.getNickname() != null) {
      user.updateNickname(request.getNickname());
    }

    // 성별 업데이트 (최초 설정만 가능)
    if (request.getGender() != null) {
      try {
        user.updateGender(request.getGender());
      } catch (IllegalStateException e) {
        log.warn(
            "성별 수정 시도 무시됨: userId={}, 기존 gender={}, 요청 gender={}",
            userId,
            user.getGender(),
            request.getGender());
        // 예외를 던지지 않고 무시 (프론트에서는 성공으로 처리)
      }
    }

    // 생년 업데이트 (최초 설정만 가능)
    if (request.getBirthYear() != null) {
      try {
        user.updateBirthYear(request.getBirthYear());
      } catch (IllegalStateException e) {
        log.warn(
            "생년 수정 시도 무시됨: userId={}, 기존 birthYear={}, 요청 birthYear={}",
            userId,
            user.getBirthYear(),
            request.getBirthYear());
        // 예외를 던지지 않고 무시 (프론트에서는 성공으로 처리)
      }
    }

    // 직업 업데이트
    if (request.getJobs() != null) {
      Set<JobType> jobs = EnumConverterUtil.convertJobsToEnum(request.getJobs());
      user.updateJobs(jobs);
    }

    // 질환 업데이트
    if (request.getDisorders() != null) {
      Set<DisorderType> disorders =
          EnumConverterUtil.convertDisordersToEnum(request.getDisorders());
      user.updateDisorders(disorders);
    }

    // 증상 업데이트
    if (request.getSymptoms() != null) {
      Set<SymptomType> symptoms = EnumConverterUtil.convertSymptomsToEnum(request.getSymptoms());
      user.updateSymptoms(symptoms);
    }

    userRepository.save(user);

    return new UserProfileResponse(user);
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getUserProfile(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    return new UserProfileResponse(user);
  }

  @Transactional
  public UserResetResponse resetUserData(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 1. 복약 기록 삭제
    medicationLogRepository.deleteByUserId(userId);

    // 2. 사용자 프로필 데이터 초기화 (로그인 정보는 유지)
    user.resetUserData();
    userRepository.save(user);

    log.info("User data reset completed for userId: {}", userId);

    return UserResetResponse.success();
  }

  @Transactional
  public UserDeleteResponse deactivateUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 이미 탈퇴 처리된 계정인지 확인
    if (user.isDeactivated()) {
      throw new IllegalStateException("이미 탈퇴 처리된 계정입니다.");
    }

    // 1. 사용자 연관 데이터 즉시 삭제
    deleteUserRelatedData(userId);

    // 2. 계정 비활성화 (30일 유예 기간)
    user.deactivateAccount();
    userRepository.save(user);

    log.info(
        "User account deactivated and related data deleted for userId: {}. Account will be"
            + " permanently deleted after 30 days.",
        userId);

    return UserDeleteResponse.success();
  }

  @Transactional
  public void deleteUserRelatedData(Long userId) {
    log.info("사용자 연관 데이터 삭제 시작: userId={}", userId);

    try {
      // 1. RefreshToken 삭제
      refreshTokenRepository.deleteByUserId(userId);
      log.info("RefreshToken 삭제 완료: userId={}", userId);

      // 2. BlacklistedToken 삭제
      blacklistService.removeUserTokens(userId);
      log.info("BlacklistedToken 삭제 완료: userId={}", userId);

      // 3. MedicationLog 삭제 (MedicationBundle을 통한 연관 삭제)
      medicationLogRepository.deleteByUserId(userId);
      log.info("MedicationLog 삭제 완료: userId={}", userId);

      // 4. MedicationBundle 삭제
      medicationBundleRepository.deleteByUserId(userId);
      log.info("MedicationBundle 삭제 완료: userId={}", userId);

      // TODO: ChatSession 및 관련 채팅 데이터 삭제 (Repository가 필요하면 추가)
      // deleteChatRelatedData(userId);

      log.info("사용자 연관 데이터 삭제 완료: userId={}", userId);
    } catch (Exception e) {
      log.error("사용자 연관 데이터 삭제 중 오류 발생: userId={}", userId, e);
      throw new RuntimeException("연관 데이터 삭제 중 오류가 발생했습니다.", e);
    }
  }
}
