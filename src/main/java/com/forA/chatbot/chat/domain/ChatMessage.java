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
@Document(collection = "chat_messages")
public class ChatMessage { // 챗봇 및 유저의 모든 메시지를 순서대로 기록

  public enum SenderType { // 누가 보낸 메시지인지 구분
    USER,
    BOT,
    SYSTEM
  }

  @Id private String id;

  @Field(name = "session_id")
  private String sessionId; // ChatSession과의 연결 고리

  @Field(name = "sender_type")
  private SenderType senderType;

  // 메시지가 발생한 시점의 채팅 단계 (5.1.1, 5.2.2 등)
  @Field(name = "chat_step")
  private String chatStep;

  @Field(name = "message_content")
  private String messageContent; // 실제 메시지 내용

  @Field(name = "sent_at")
  @Builder.Default
  private LocalDateTime sentAt = LocalDateTime.now();

  // 유저 응답 메시지인 경우, 유저가 선택한 옵션의 코드 등을 저장 가능 (선택 사항)
  @Field(name = "response_code")
  private String responseCode;
}
