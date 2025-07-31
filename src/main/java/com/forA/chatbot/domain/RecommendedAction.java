package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "recommended_actions")
public class RecommendedAction extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @Column(name = "action_code", nullable = false, length = 50)
    private String actionCode;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;
}
