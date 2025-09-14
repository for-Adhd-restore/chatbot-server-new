package com.forA.chatbot.medicationItems;

import com.forA.chatbot.global.BaseTimeEntity;
import com.forA.chatbot.medicationBundles.MedicationBundle;
import jakarta.persistence.*;

@Entity
@Table(name = "medication_items")
public class MedicationItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "medication_bundle_id", nullable = false)
  private MedicationBundle medicationBundle;

  @Column(name = "medication_name", nullable = false, length = 50)
  private String medicationName;
}
