package com.akgeneralstore.security;

import com.akgeneralstore.config.RateLimitProperties;
import com.akgeneralstore.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        String clientKey = resolveClientKey(request, rule.bucketName());
        long now = Instant.now().getEpochSecond();

        WindowCounter counter = counters.compute(clientKey, (key, existing) -> {
            if (existing == null || existing.windowStartEpochSecond + 60 <= now) {
                return new WindowCounter(now, 1);
            }
            existing.count++;
            return existing;
        });

        int remaining = Math.max(rule.limit() - counter.count, 0);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (counter.count > rule.limit()) {
            long retryAfter = Math.max(1, 60 - (now - counter.windowStartEpochSecond));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            objectMapper.writeValue(
                    response.getWriter(),
                    new ApiResponse<>(false, rule.message(), null)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (
                path.startsWith("/api/auth/request-otp")
                        || path.startsWith("/api/auth/request-login-otp")
                        || path.startsWith("/api/auth/forgot-password")
        ) {
            return new RateLimitRule("otp", properties.getOtpRequestsPerMinute(), "Too many OTP or reset requests. Please wait before trying again.");
        }

        if (path.startsWith("/api/auth/")) {
            return new RateLimitRule("auth", properties.getAuthRequestsPerMinute(), "Too many authentication requests. Please wait before trying again.");
        }

        if (path.startsWith("/api/admin/products/upload-image") || path.startsWith("/api/admin/categories/upload-image")) {
            return new RateLimitRule("upload", properties.getUploadRequestsPerMinute(), "Too many upload attempts. Please slow down and try again.");
        }

        return new RateLimitRule("api", properties.getApiRequestsPerMinute(), "Too many API requests. Please wait before trying again.");
    }

    private String resolveClientKey(HttpServletRequest request, String bucketName) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor != null && !forwardedFor.isBlank()
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return bucketName + ":" + ip;
    }

    private record RateLimitRule(String bucketName, int limit, String message) {}

    private static final class WindowCounter {
        private final long windowStartEpochSecond;
        private int count;

        private WindowCounter(long windowStartEpochSecond, int count) {
            this.windowStartEpochSecond = windowStartEpochSecond;
            this.count = count;
        }
    }
}
