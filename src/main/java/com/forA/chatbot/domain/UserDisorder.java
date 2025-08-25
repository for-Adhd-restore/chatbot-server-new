package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_disorders")
public class UserDisorder extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "disorder_id", nullable = false)
    private Disorder disorder;
}
