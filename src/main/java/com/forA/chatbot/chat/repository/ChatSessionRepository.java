package com.forA.chatbot.chat.repository;

import com.forA.chatbot.chat.domain.ChatSession;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
  // 유저의 현재 활성(onboardingCompleted = false) 세션 찾기
  Optional<ChatSession> findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(Long userId);

  // 유저의 가장 최근 세션 찾기 (Onboarding 재진입 시 사용)
  Optional<ChatSession> findFirstByUserIdOrderByStartedAtDesc(Long userId);
}
