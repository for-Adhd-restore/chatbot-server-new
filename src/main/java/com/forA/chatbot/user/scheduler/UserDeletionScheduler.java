package com.forA.chatbot.user.scheduler;

import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.medications.repository.MedicationLogRepository;
import com.forA.chatbot.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDeletionScheduler {

    private final UserRepository userRepository;
    private final MedicationLogRepository medicationLogRepository;

    /**
     * 매일 새벽 2시에 30일이 지난 탈퇴 계정을 완전히 삭제합니다.
     */
    @Scheduled(cron = "0 0 2 * * *") // 매일 02:00
    @Transactional
    public void deleteExpiredUsers() {
        log.info("Starting scheduled deletion of expired user accounts");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<User> usersToDelete = userRepository.findUsersToBeDeleted(cutoffDate);

        if (usersToDelete.isEmpty()) {
            log.info("No expired user accounts found for deletion");
            return;
        }

        int deletedCount = 0;
        for (User user : usersToDelete) {
            try {
                // 1. 관련 데이터 삭제
                medicationLogRepository.deleteByUserId(user.getId());
                
                // TODO: 추후 다른 관련 데이터 삭제 로직 추가
                // chatSessionRepository.deleteByUserId(user.getId());
                // selectedEmotionRepository.deleteByUserId(user.getId());
                // actionFeedbackRepository.deleteByUserId(user.getId());

                // 2. 사용자 완전 삭제
                userRepository.delete(user);
                
                deletedCount++;
                log.info("Permanently deleted user account: userId={}, deletedAt={}", 
                         user.getId(), user.getDeletedAt());
                         
            } catch (Exception e) {
                log.error("Failed to delete user account: userId={}, error={}", 
                         user.getId(), e.getMessage(), e);
            }
        }

        log.info("Completed scheduled deletion. Total deleted accounts: {}", deletedCount);
    }

    /**
     * 매주 월요일 오전 9시에 30일 이내에 삭제될 계정 통계를 로깅합니다. (모니터링 목적)
     */
    @Scheduled(cron = "0 0 9 * * MON") // 매주 월요일 09:00
    public void logPendingDeletions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        LocalDateTime futureDate = LocalDateTime.now().minusDays(23); // 7일 후 삭제될 계정들

        List<User> soonToBeDeleted = userRepository.findUsersToBeDeleted(futureDate);
        List<User> expiredUsers = userRepository.findUsersToBeDeleted(cutoffDate);

        log.info("User deletion statistics - Expired (ready for deletion): {}, " +
                "Will expire in 7 days: {}", 
                expiredUsers.size(), soonToBeDeleted.size() - expiredUsers.size());
    }
}