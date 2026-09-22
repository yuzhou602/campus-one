package com.campusone.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistCleanupJob {
    private final TokenBlacklistMapper tokenBlacklistMapper;

    @Scheduled(cron = "${jwt.blacklist-cleanup-cron:0 15 * * * *}")
    public void removeExpiredTokens() {
        int removed = tokenBlacklistMapper.delete(
                new LambdaQueryWrapper<TokenBlacklist>()
                        .lt(TokenBlacklist::getExpiresAt, LocalDateTime.now()));
        if (removed > 0) {
            log.info("Removed {} expired token blacklist entries", removed);
        }
    }
}
