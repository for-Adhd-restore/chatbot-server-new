package com.forA.chatbot.chat.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chat_session_id", nullable = false)
  private ChatSession chatSession;

  @Column(length = 10, nullable = false)
  private String sender;

  @Column(length = 50, nullable = false)
  private String step;

  @Column(nullable = false)
  private String message;

  @Column(name = "sended_at", nullable = false)
  private LocalDateTime sendedAt;

}
