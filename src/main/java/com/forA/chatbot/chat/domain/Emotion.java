package com.forA.chatbot.chat.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "emotions")
public class Emotion extends BaseTimeEntity {

  @Id
  private Long id;

  @Column(name = "emotion_type", nullable = false, length = 50)
  private String emotionType;

  @Column(name = "emotion_code", nullable = false, length = 50)
  private String emotionCode;

  @Column(name = "emotion_name_ko", nullable = false, length = 50)
  private String emotionNameKo;
}
