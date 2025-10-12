package com.forA.chatbot.user.domain;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.global.BaseTimeEntity;
import com.forA.chatbot.user.converter.DisorderSetConverter;
import com.forA.chatbot.user.converter.JobSetConverter;
import com.forA.chatbot.user.converter.SymptomSetConverter;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Builder를 위한 전체 필드 생성자 추가
@Builder // Builder 패턴 사용
@Getter
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  // --- Apple 로그인으로 추가될 필드 ---
  @Column(nullable = false, unique = true)
  private String email; // 이메일 필드 추가

  @Getter
  @Column(unique = true)
  private String appleUniqueId;

  @Column(length = 10, nullable = false)
  @Builder.Default
  private Gender gender = Gender.UNKNOWN;

  @Column(name = "birth_year")
  private Integer birthYear;

  @Column(length = 20)
  private String nickname;

  @Column(name = "last_name", length = 20)
  private String lastName;

  @Column(name = "first_name", length = 30)
  private String firstName;

  @Column(name = "full_name", length = 50, nullable = false)
  private String fullName;

  @Convert(converter = JobSetConverter.class)
  @Column(columnDefinition = "JSON")
  @Size(max = 2, message = "최대 2개의 job만 선택할 수 있습니다")
  @Builder.Default
  private Set<JobType> jobs = new HashSet<>();

  @Convert(converter = DisorderSetConverter.class)
  @Column(columnDefinition = "JSON")
  @Size(max = 2, message = "최대 2개의 disorder만 선택할 수 있습니다")
  @Builder.Default
  private Set<DisorderType> disorders = new HashSet<>();

  @Convert(converter = SymptomSetConverter.class)
  @Column(columnDefinition = "JSON")
  @Size(max = 2, message = "최대 2개의 symptom만 선택할 수 있습니다")
  @Builder.Default
  private Set<SymptomType> symptoms = new HashSet<>();

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "deleated_at")
  private LocalDateTime deletedAt;

  @Column(name = "is_notification_enabled")
  @Builder.Default
  private Boolean isNotificationEnabled = false;

  public void updateNickname(String nickname) {
    this.nickname = nickname;
  }

  /*
   * 성별 업데이트 (최초 1회만 가능)
   * 이미 설정된 경우 예외 발생
   * */
  public void updateGender(Gender gender) {
    // Gender가 UNKNOWN이 아니고 null도 아닌 경우 수정 불가
    if (this.gender != null && this.gender != Gender.UNKNOWN) {
      throw new UserHandler(ErrorStatus.USER_GENDER_ALREADY_EXIST);
    }
    this.gender = gender;
  }

  /** 생년 업데이트 (최초 1회만 가능) 이미 설정된 경우 예외 발생 */
  public void updateBirthYear(Integer birthYear) {
    // 이미 설정된 경우 수정 불가
    if (this.birthYear != null) {
      throw new UserHandler(ErrorStatus.USER_BIRTH_YEAR_ALREADY_EXIST);
    }
    this.birthYear = birthYear;
  }

  public void updateJobs(Set<JobType> jobs) {
    this.jobs = jobs != null ? jobs : new HashSet<>();
  }

  public void updateDisorders(Set<DisorderType> disorders) {
    this.disorders = disorders != null ? disorders : new HashSet<>();
  }

  public void updateSymptoms(Set<SymptomType> symptoms) {
    this.symptoms = symptoms != null ? symptoms : new HashSet<>();
  }

  public void resetUserData() {
    this.nickname = null;
    this.gender = Gender.UNKNOWN;
    this.birthYear = null;
    this.jobs = new HashSet<>();
    this.disorders = new HashSet<>();
    this.symptoms = new HashSet<>();
  }

  public void deactivateAccount() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
  }

  public boolean isDeactivated() {
    return this.isDeleted != null && this.isDeleted;
  }

  public boolean isPermanentlyDeletable() {
    if (!isDeactivated() || this.deletedAt == null) {
      return false;
    }
    // 30일 후 완전 삭제 가능
    return LocalDateTime.now().isAfter(this.deletedAt.plusDays(30));
  }
}
