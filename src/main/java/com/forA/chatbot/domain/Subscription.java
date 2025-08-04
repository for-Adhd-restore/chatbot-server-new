package com.forA.chatbot.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "original_transaction_id", nullable = false)
    private String originalTransactionId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "is_auto_renew", nullable = false)
    private Boolean isAutoRenew;

    @Column(name = "is_trial", nullable = false)
    private Boolean isTrial;

}

