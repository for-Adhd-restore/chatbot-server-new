package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_symptoms")
public class UserSymptom extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "symptom_id", nullable = false)
    private Symptom symptom;
}
