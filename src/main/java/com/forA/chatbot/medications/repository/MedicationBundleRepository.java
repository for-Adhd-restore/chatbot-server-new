package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicationBundleRepository extends JpaRepository<MedicationBundle, Long> {
  List<MedicationBundle> findByUserId(Long userId);
}
