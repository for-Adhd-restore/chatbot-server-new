package com.forA.chatbot.notification.repository;

import com.forA.chatbot.notification.domain.DeviceToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  // 토큰 문자열로 엔티티 조회
  Optional<DeviceToken> findByDeviceToken(String deviceToken);

  // 특정 시간 이전에 업데이트된 모든 토큰 삭제 (스케줄러에서 사용)
  void deleteByLastUpdatedAtBefore(LocalDateTime expiryDate);
}
