package com.forA.chatbot.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_responses")
public class UserResponse extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @Column(length = 50, nullable = false)
    private String step;

    @Column(name = "response_value", nullable = false)
    private String responseValue;

    @Column(nullable = false)
    private LocalDateTime respondedAt;


}
