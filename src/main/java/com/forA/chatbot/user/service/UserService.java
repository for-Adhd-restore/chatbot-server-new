package com.forA.chatbot.user.service;

import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;
import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import com.forA.chatbot.user.dto.NicknameResponse;
import com.forA.chatbot.user.dto.UserProfileResponse;
import com.forA.chatbot.user.dto.UserProfileUpdateRequest;
import com.forA.chatbot.user.dto.UserResetResponse;
import com.forA.chatbot.user.dto.UserDeleteResponse;
import com.forA.chatbot.user.util.EnumConverterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final MedicationLogRepository medicationLogRepository;

  @Transactional
  public NicknameResponse updateNickname(Long userId, String nickname) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    user.updateNickname(nickname);
    userRepository.save(user);

    return new NicknameResponse(nickname);
  }

  @Transactional
  public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 닉네임 업데이트
    if (request.getNickname() != null) {
      user.updateNickname(request.getNickname());
    }

    // 성별 업데이트
    if (request.getGender() != null) {
      user.updateGender(request.getGender());
    }

    // 생년 업데이트
    if (request.getBirthYear() != null) {
      user.updateBirthYear(request.getBirthYear());
    }

    // 직업 업데이트
    if (request.getJobs() != null) {
      Set<JobType> jobs = EnumConverterUtil.convertJobsToEnum(request.getJobs());
      user.updateJobs(jobs);
    }

    // 질환 업데이트
    if (request.getDisorders() != null) {
      Set<DisorderType> disorders = EnumConverterUtil.convertDisordersToEnum(request.getDisorders());
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
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    return new UserProfileResponse(user);
  }

  @Transactional
  public UserResetResponse resetUserData(Long userId) {
    User user = userRepository.findById(userId)
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
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 이미 탈퇴 처리된 계정인지 확인
    if (user.isDeactivated()) {
      throw new IllegalStateException("이미 탈퇴 처리된 계정입니다.");
    }

    // 계정 비활성화 (30일 유예 기간)
    user.deactivateAccount();
    userRepository.save(user);

    log.info("User account deactivated for userId: {}. Will be permanently deleted after 30 days.", userId);

    return UserDeleteResponse.success();
  }
}
