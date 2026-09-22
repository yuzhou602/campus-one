package com.campusone.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_REQUESTS_PER_MINUTE = 10;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        int limit = uri.contains("/auth/login") ? MAX_LOGIN_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
        String bucket = uri.contains("/auth/login") ? "login" : "api";
        String key = bucket + ":" + getClientKey(request);

        RateLimitInfo info = requestCounts.computeIfAbsent(key, k -> new RateLimitInfo());
        long now = System.currentTimeMillis();
        if (requestCounts.size() > 10_000) {
            requestCounts.entrySet().removeIf(entry -> now - entry.getValue().windowStart > 120_000);
        }

        if (now - info.windowStart > 60_000) {
            info.count.set(0);
            info.windowStart = now;
        }

        if (info.count.incrementAndGet() > limit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            } catch (Exception ignored) {}
            return false;
        }
        return true;
    }

    private String getClientKey(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        String xff = request.getHeader("X-Forwarded-For");
        String remoteAddress = request.getRemoteAddr();
        boolean fromTrustedLocalProxy = "127.0.0.1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress);
        if (fromTrustedLocalProxy && xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return remoteAddress;
    }

    static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }
}
