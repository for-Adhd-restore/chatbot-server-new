package com.forA.chatbot.user;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "disorders")
public class Disorder extends BaseTimeEntity {

  @Id
  private Long id;

  @Column(name = "disorder_code", length = 50, nullable = false)
  private String disorderCode;

  @Column(name = "disorder_name_ko", length = 50, nullable = false)
  private String disorderNameKo;
}
