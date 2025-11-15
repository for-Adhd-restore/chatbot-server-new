package com.forA.chatbot.notification.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import com.forA.chatbot.user.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "device_tokens")
public class DeviceToken extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "device_token", nullable = false, unique = true)
  private String deviceToken;

  @UpdateTimestamp // JPA Dirty Checking에 의해 업데이트 시 자동으로 시간이 기록됨
  @Column(name = "last_updated_at", nullable = false)
  private LocalDateTime lastUpdatedAt;

  public DeviceToken(User user, String deviceToken) {
    this.user = user;
    this.deviceToken = deviceToken;
  }
}
