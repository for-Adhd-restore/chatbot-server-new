package com.forA.chatbot.chat.repository;

import com.forA.chatbot.chat.domain.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
  Optional<ChatSession> findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(Long userId);
  Optional<ChatSession> findFirstByUserIdOrderByStartedAtDesc(Long userId);
  Optional<ChatSession> findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(Long userId);


  List<ChatSession> findByUserIdAndStartedAtBetween(
      Long userId, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime);
}
