package com.forA.chatbot.domain;


import com.forA.chatbot.domain.enums.ProviderType;

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
    private ProviderType providerType;

    @Column(name = "provider_user_id", length = 100, nullable = false)
    private String providerUserId;
}
