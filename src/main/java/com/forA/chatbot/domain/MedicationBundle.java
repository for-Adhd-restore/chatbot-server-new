package com.forA.chatbot.domain;

import jakarta.persistence.*;

import java.sql.Time;

@Entity
@Table(name = "medication_bundles")
public class MedicationBundle extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bundle_name", nullable = false, length = 50)
    private String bundleName;

    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek;

    @Column(name = "scheduled_time")
    private Time scheduledTime;

    @Column(name = "alarm_enabled", nullable = false)
    private Boolean alarmEnabled;

    @Column(name = "alarm_time")
    private Time alarmTime;


}
