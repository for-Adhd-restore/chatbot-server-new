package com.forA.chatbot.chat.repository;

import com.forA.chatbot.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
  // 특정 세션의 모든 메시지를 보낸 시간 순으로 조회
  List<ChatMessage> findBySessionIdOrderBySentAtAsc(String sessionId);

  // 특정 세션의 메시지 개수 조회
  long countBySessionId(String sessionId);

}
