package com.forA.chatbot.chat.repository;

import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatMessage.SenderType;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
  // 특정 세션의 모든 메시지를 보낸 시간 순으로 조회
  List<ChatMessage> findBySessionIdOrderBySentAtAsc(String sessionId);

  // 특정 세션의 메시지 개수 조회
  long countBySessionId(String sessionId);

  /**
   * 특정 세션의 특정 발신자 타입과 챗봇 단계에 해당하는 메시지 조회
   * - 감정 리포트에서 USER가 선택한 감정을 찾기 위해 사용
   */
  List<ChatMessage> findBySessionIdAndSenderTypeAndChatStep(
      String sessionId, SenderType senderType, String chatStep);
  /**
   * 여러 세션 ID에 속하는 모든 메시지를 시간순으로 정렬하여 반환
   */
  List<ChatMessage> findBySessionIdInOrderBySentAtAsc(List<String> sessionIds);
}