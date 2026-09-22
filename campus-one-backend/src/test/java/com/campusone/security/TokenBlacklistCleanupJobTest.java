package com.campusone.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenBlacklistCleanupJobTest {
    @Test
    @SuppressWarnings("unchecked")
    void removesExpiredEntries() {
        TokenBlacklistMapper mapper = mock(TokenBlacklistMapper.class);
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);

        new TokenBlacklistCleanupJob(mapper).removeExpiredTokens();

        verify(mapper).delete(any(LambdaQueryWrapper.class));
    }
}
