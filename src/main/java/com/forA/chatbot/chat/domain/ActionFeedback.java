package com.forA.chatbot.chat.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "action_feedbacks")
public class ActionFeedback extends BaseTimeEntity { // 제안된 행동 수행 여부 및 만족도 피드백 기록

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chat_session_id", nullable = false)
  private ChatSession chatSession;

  @Column(name = "did_perform", nullable = false)
  private Boolean didPerform;

  @Column(name = "emotion_score")
  private Integer emotionScore;
}
