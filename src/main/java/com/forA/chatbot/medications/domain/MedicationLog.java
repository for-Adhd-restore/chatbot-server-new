package com.forA.chatbot.medications.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Time;

@Entity
@Table(name = "medication_logs")
public class  MedicationLog extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "medication_bundle_id")
  private MedicationBundle medicationBundle;

  @Column(nullable = false)
  private Date date;

  @Column(name = "is_taken", nullable = false)
  private Boolean isTaken;

  @Column(name = "taken_at")
  private Time takenAt;

  @Column(length = 50, name = "med_condition")
  private String medCondition;
}
