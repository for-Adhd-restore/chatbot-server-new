package com.forA.chatbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "disorder_symtoms")
public class Symtom extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "disroder_id")
    private Disorder disorder;

    @Column(name = "symtom_code", length = 50, nullable = false)
    private String symtomCode;

    @Column(name = "symtom_text_ko", nullable = false)
    private String symtomTextKo;
}
