package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicationItemRepository extends JpaRepository<MedicationItem, Long> {
  List<MedicationItem> findByMedicationBundleId(Long medicationBundleId);
}
