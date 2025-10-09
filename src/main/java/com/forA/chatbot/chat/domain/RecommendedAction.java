package com.forA.chatbot.chat.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recommended_actions")
public class RecommendedAction extends BaseTimeEntity { // 챗봇이 제안한 행동 기록

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chat_session_id", nullable = false)
  private ChatSession chatSession;

  @Column(name = "action_code", nullable = false, length = 50)
  private String actionCode;

  @Column(name = "is_selected", nullable = false)
  private Boolean isSelected;
}
