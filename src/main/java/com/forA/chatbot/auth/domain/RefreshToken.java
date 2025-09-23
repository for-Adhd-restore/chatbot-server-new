package com.forA.chatbot.auth.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Builder
@NoArgsConstructor // JPA를 위한 기본 생성자
@AllArgsConstructor // @Builder를 위한 모든 필드 생성자
@Data
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
