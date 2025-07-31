package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "action_feedbacks")
public class ActionFeedback extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @Column(name = "did_perform", nullable = false)
    private Boolean didPerform;

    @Column(name = "emotion_score")
    private Integer emotionScore;
}
