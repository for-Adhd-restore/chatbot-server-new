package com.forA.chatbot.chat.repository;

import com.forA.chatbot.chat.domain.ChatSession;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
  Optional<ChatSession> findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(Long userId);
  Optional<ChatSession> findFirstByUserIdOrderByStartedAtDesc(Long userId);
  Optional<ChatSession> findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(Long userId);
}
