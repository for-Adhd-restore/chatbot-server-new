package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_symtoms")
public class UserSymtom extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "symtom_id")
    private Symtom symtom;
}
