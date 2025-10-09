package com.forA.chatbot.chat.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "selected_emotions")
public class SelectedEmotion extends BaseTimeEntity { // //유저가 선택한 감정 기록.

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chat_session_id", nullable = false)
  private ChatSession chatSession;

  @ManyToOne
  @JoinColumn(name = "emotion_id", nullable = false)
  private Emotion emotion;
}
