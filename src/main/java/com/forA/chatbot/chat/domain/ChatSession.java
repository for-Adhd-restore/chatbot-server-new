package com.forA.chatbot.chat.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@Document(collection = "chat_sessions")
public class ChatSession {

  @Id private String id; // MongoDB의 ObjectId

  @Field(name = "user_id")
  private Long userId; // JPA User 엔티티와의 연결 고리

  // 유저 기본 데이터 수집
  @Field(name = "current_step")
  private String currentStep;

  // 데이터 수집 완료 여부.
  @Field(name = "onboarding_completed")
  @Builder.Default
  private Boolean onboardingCompleted = false;

  // 채팅 세션의 시작 시간
  @Field(name = "started_at")
  private LocalDateTime startedAt;

  // 채팅 세션의 종료 시간 (비정상 종료 포함)
  @Field(name = "ended_at")
  private LocalDateTime endedAt;

  // 가장 마지막으로 채팅 기록이 저장된 시간 (재접속 시점 추론에 사용)
  @Field(name = "last_interaction_at")
  private LocalDateTime lastInteractionAt;
}