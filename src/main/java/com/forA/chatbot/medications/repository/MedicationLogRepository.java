package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationBundle;
import com.forA.chatbot.medications.domain.MedicationLog;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

  @Modifying
  @Transactional
  @Query("DELETE FROM MedicationLog ml WHERE ml.medicationBundle.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  Optional<MedicationLog> findByMedicationBundleAndDate(
      MedicationBundle medicationBundle, Date date);

  @Query(
      "SELECT ml FROM MedicationLog ml WHERE ml.medicationBundle.user.id = :userId AND ml.date ="
          + " :date")
  List<MedicationLog> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") Date date);
}
