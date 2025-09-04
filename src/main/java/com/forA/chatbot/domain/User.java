package com.forA.chatbot.domain;

import com.forA.chatbot.domain.enums.Gender;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)

    private Gender gender =  Gender.UNKNOWN;

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

    @Column(columnDefinition = "JSON")
    @Size(max = 2, message = "최대 2개의 job만 선택할 수 있습니다")
    private List<Integer> job = new ArrayList<>();

    @Column(columnDefinition = "JSON")
    @Size(max = 2, message = "최대 2개의 disorder만 선택할 수 있습니다")
    private List<Image> disorder = new ArrayList<>();
    @Column(columnDefinition = "JSON")
    @Size(max = 2, message = "최대 2개의 symptom만 선택할 수 있습니다")
    private List<Integer> symptom =  new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleated_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_notification_enabled")
    private Boolean isNotificationEnabled = false;
}