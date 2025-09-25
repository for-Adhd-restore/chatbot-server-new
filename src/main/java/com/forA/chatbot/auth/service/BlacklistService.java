package com.forA.chatbot.auth.service;

import com.forA.chatbot.auth.domain.BlacklistedToken;
import com.forA.chatbot.auth.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Transactional
    public void addToBlacklist(String token, LocalDateTime expiresAt, Long userId) {
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .userId(userId)
                .build();
        
        blacklistedTokenRepository.save(blacklistedToken);
        log.info("토큰이 블랙리스트에 추가됨: userId={}", userId);
    }

    public boolean isBlacklisted(String token) {
        boolean blacklisted = blacklistedTokenRepository.existsByToken(token);
        if (blacklisted) {
            log.warn("블랙리스트된 토큰 접근 시도: token={}", token.substring(0, 20) + "...");
        }
        return blacklisted;
    }

    @Transactional
    public void removeUserTokens(Long userId) {
        blacklistedTokenRepository.deleteByUserId(userId);
        log.info("사용자의 모든 블랙리스트 토큰 제거: userId={}", userId);
    }

    // 매일 새벽 2시에 만료된 토큰 정리
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        blacklistedTokenRepository.deleteExpiredTokens(now);
        log.info("만료된 블랙리스트 토큰 정리 완료: {}", now);
    }
}