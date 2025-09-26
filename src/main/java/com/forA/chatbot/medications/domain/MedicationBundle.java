package com.forA.chatbot.medications.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import com.forA.chatbot.user.User;
import jakarta.persistence.*;
import java.sql.Time;
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
  private Time scheduledTime;

  @Column(name = "alarm_enabled", nullable = false)
  private Boolean alarmEnabled;

  @Column(name = "alarm_time")
  private Time alarmTime;

  @Builder
  public MedicationBundle(
      User user,
      String bundleName,
      String dayOfWeek,
      Time scheduledTime,
      Boolean alarmEnabled,
      Time alarmTime) {
    this.user = user;
    this.bundleName = bundleName;
    this.dayOfWeek = dayOfWeek;
    this.scheduledTime = scheduledTime;
    this.alarmEnabled = alarmEnabled;
    this.alarmTime = alarmTime;
  }
}
