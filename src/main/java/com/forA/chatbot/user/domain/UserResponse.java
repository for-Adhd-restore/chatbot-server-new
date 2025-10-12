package com.forA.chatbot.user.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_responses")
public class UserResponse extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 50, nullable = false)
  private String step;

  @Column(name = "response_value", nullable = false)
  private String responseValue;

  @Column(nullable = false)
  private LocalDateTime respondedAt;
}
