package com.forA.chatbot.medications.repository;

import com.forA.chatbot.medications.domain.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {
    
    void deleteByUserId(Long userId);
}
