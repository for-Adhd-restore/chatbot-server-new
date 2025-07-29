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