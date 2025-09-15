package com.forA.chatbot.auth.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;

@Entity
@Table(name = "refresh_tokens")
@Builder
public class RefreshToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private Long userId;

  private LocalDateTime expiresAt;
}
