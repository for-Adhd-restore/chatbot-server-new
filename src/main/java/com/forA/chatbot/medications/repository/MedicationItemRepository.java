package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationItemRepository extends JpaRepository<MedicationItem, Long> {
  List<MedicationItem> findByMedicationBundleId(Long medicationBundleId);
}
