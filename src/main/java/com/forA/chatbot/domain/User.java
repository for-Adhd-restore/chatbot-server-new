package com.forA.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Builder를 위한 전체 필드 생성자 추가
@Builder // Builder 패턴 사용
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    // --- Apple 로그인으로 추가될 필드 ---
    @Column(nullable = false, unique = true)
    private String email; // 이메일 필드 추가

    @Getter
    @Column(unique = true)
    private String appleUniqueId;

    @Column(length = 10, nullable = false)
    private String gender = "UNKNOWN";

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(length = 20)
    private String nickname;

    @Column(name = "last_name", length = 20)
    private String lastName;

    @Column(name = "first_name", length = 30)
    private String firstName;

    @Column(name = "full_name", length = 50, nullable = false)
    private String fullName;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleated_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_notification_enabled")
    private Boolean isNotificationEnabled = false;
}