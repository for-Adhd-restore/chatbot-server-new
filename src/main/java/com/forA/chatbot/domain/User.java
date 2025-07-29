package com.forA.chatbot.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String gender = "UNKNOWN";

    private Integer birthYear;

    @Column(length = 20)
    private String nickname;

    @Column(length = 20)
    private String lastName;

    @Column(length = 30)
    private String firstName;

    @Column(length = 50, nullable = false)
    private String fullName;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleated_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_notification_enabled")
    private Boolean isNotificationEnabled = false;

}