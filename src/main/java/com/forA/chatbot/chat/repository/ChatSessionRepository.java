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

  /**
   * 특정 사용자의 특정 단계이고 특정 날짜 범위에 생성된 세션 조회
   * - 감정 리포트에서 EMOTION_SELECT 단계의 세션을 찾기 위해 사용
   */
  @Query(
      "{ 'user_id': ?0, 'current_step': ?1, 'started_at': { $gte: ?2, $lt: ?3 } }")
  List<ChatSession> findByUserIdAndCurrentStepAndDateRange(
      Long userId, String currentStep, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime);

}
