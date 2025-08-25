package com.forA.chatbot.domain;

import com.forA.chatbot.domain.enums.Status;
import jakarta.persistence.*;

import java.time.LocalDateTime;

public class Subscriptions extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "original_transaction_id", nullable = false)
    private Long originalTransactionId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "status", nullable = false)
    private Status status;
    @Column(name = "is_auto_renew", nullable = false)
    private Boolean isAutoRenew;
    @Column(name = "is_trial", nullable = false)
    private Boolean isTrial;
}
