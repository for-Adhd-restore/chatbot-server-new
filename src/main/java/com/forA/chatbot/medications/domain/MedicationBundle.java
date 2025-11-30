package com.forA.chatbot.medications.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import com.forA.chatbot.user.domain.User;
import jakarta.persistence.*;
import java.sql.Time;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "medication_bundles")
public class MedicationBundle extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "bundle_name", nullable = false, length = 50)
  private String bundleName;

  @Column(name = "day_of_week")
  private String dayOfWeek;

  @Column(name = "scheduled_time")
  private LocalTime scheduledTime;

  @Column(name = "alarm_enabled", nullable = false)
  private Boolean alarmEnabled;

  @Column(name = "alarm_time")
  private LocalTime alarmTime;

  @Column(name = "is_deleted")
  private Boolean isDeleted = false;

  @Builder
  public MedicationBundle(
      User user,
      String bundleName,
      String dayOfWeek,
      LocalTime scheduledTime,
      Boolean alarmEnabled,
      LocalTime alarmTime) {
    this.user = user;
    this.bundleName = bundleName;
    this.dayOfWeek = dayOfWeek;
    this.scheduledTime = scheduledTime;
    this.alarmEnabled = alarmEnabled;
    this.alarmTime = alarmTime;
    this.isDeleted = false;
  }

  // 복용 계획 업데이트 메서드
  public void updateMedicationPlan(
      String bundleName,
      String dayOfWeek,
      LocalTime scheduledTime,
      Boolean alarmEnabled,
      LocalTime alarmTime) {
    this.bundleName = bundleName;
    this.dayOfWeek = dayOfWeek;
    this.scheduledTime = scheduledTime;
    this.alarmEnabled = alarmEnabled;
    this.alarmTime = alarmTime;
  }

  public void softDelete() {
    this.isDeleted = true;
  }
}
