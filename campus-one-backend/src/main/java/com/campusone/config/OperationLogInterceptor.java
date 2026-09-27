package com.campusone.config;

import com.campusone.system.log.entity.OperationLog;
import com.campusone.system.log.mapper.OperationLogMapper;
import com.campusone.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {
    private final OperationLogMapper logMapper;
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startedAt = startTime.get();
        startTime.remove();
        if (startedAt == null) return;
        Long duration = System.currentTimeMillis() - startedAt;

        try {
            String method = request.getMethod();
            String url = request.getRequestURI();
            if (!url.startsWith("/api/") || "GET".equals(method)) {
                return;
            }

            OperationLog opLog = new OperationLog();
            opLog.setUserId(resolveCurrentUserId());
            opLog.setMethod(method);
            opLog.setUrl(url);
            opLog.setIp(getClientIp(request));
            opLog.setStatus(response.getStatus());
            opLog.setDuration(duration);
            opLog.setCreatedAt(java.time.LocalDateTime.now());

            String[] parts = url.split("/");
            if (parts.length > 3) {
                opLog.setModule(parts[3]);
            }
            opLog.setAction(method);

            logMapper.insert(opLog);
        } catch (Exception e) {
            log.warn("Failed to save operation log", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // Forwarded headers must be normalized by a trusted proxy/container first;
        // never trust a client-supplied X-Forwarded-For value directly here.
        return request.getRemoteAddr();
    }

    private Long resolveCurrentUserId() {
        try {
            return UserContext.getCurrentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
