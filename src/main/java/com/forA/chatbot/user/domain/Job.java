package com.forA.chatbot.user.domain;

import com.forA.chatbot.global.BaseTimeEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class Job extends BaseTimeEntity {

  @Id private Long id;

  @Column(name = "job_name_ko", length = 20, nullable = false)
  private String jobNameKo;
}
