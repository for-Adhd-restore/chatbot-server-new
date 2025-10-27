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
  private String sessionId;
  @Field(name = "sender_type")
  private SenderType senderType;

  @Field(name = "chat_step")
  private String chatStep;

  @Field(name = "message_content")
  private String messageContent;

  @Field(name = "sent_at")
  @Builder.Default
  private LocalDateTime sentAt = LocalDateTime.now();

  @Field(name = "response_code")
  private String responseCode;
}
