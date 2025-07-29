package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_disorders")
public class UserDisorder extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "disorder_id")
    private Disorder disorder;
}
