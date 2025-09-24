package com.forA.chatbot.user.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "disorder_symptoms")
public class Symptom extends BaseTimeEntity {

  @Id private Long id;

  @ManyToOne
  @JoinColumn(name = "disorder_id", nullable = false)
  private Disorder disorder;

  @Column(name = "symptom_code", length = 50, nullable = false)
  private String symptomCode;

  @Column(name = "symptom_text_ko", nullable = false)
  private String symptomTextKo;
}
