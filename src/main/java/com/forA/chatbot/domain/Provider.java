package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "providers")
public class Provider extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "provider_type", length = 10, nullable = false)
    private String providerType;

    @Column(name = "provider_user_id", length = 100, nullable = false)
    private String providerUserId;
}
