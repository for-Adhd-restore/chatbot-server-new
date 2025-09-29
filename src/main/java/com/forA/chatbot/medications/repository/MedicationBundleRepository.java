package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationBundle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationBundleRepository extends JpaRepository<MedicationBundle, Long> {
  List<MedicationBundle> findByUserId(Long userId);

  /*오늘의 복용 계획 조회*/
  @Query(
      "SELECT mb FROM MedicationBundle mb WHERE mb.user.id = :userId AND mb.isDeleted = false AND "
          + "(mb.dayOfWeek IS NULL OR "
          + "mb.dayOfWeek = :dayOfWeek OR "
          + "mb.dayOfWeek LIKE CONCAT(:dayOfWeek, ',%') OR "
          + "mb.dayOfWeek LIKE CONCAT('%,', :dayOfWeek, ',%') OR "
          + "mb.dayOfWeek LIKE CONCAT('%,', :dayOfWeek))")
  List<MedicationBundle> findByUserIdAndDayOfWeek(
      @Param("userId") Long userId, @Param("dayOfWeek") String dayOfWeek);

  // 메서드 추가
  @Query(
      "SELECT m FROM MedicationBundle m WHERE m.user.id = :userId AND m.isDeleted = false ORDER BY"
          + " m.createdAt DESC")
  List<MedicationBundle> findActiveByUserId(Long userId);

  @Query("SELECT m FROM MedicationBundle m WHERE m.id = :id AND m.isDeleted = false")
  Optional<MedicationBundle> findActiveById(Long id);
}
