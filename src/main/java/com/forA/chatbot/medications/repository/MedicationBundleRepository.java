package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationBundle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MedicationBundleRepository extends JpaRepository<MedicationBundle, Long> {
  List<MedicationBundle> findByUserId(Long userId);

  @Modifying
  @Transactional
  @Query("DELETE FROM MedicationBundle mb WHERE mb.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);
}
