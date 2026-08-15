package com.paranietharan.byteblog.security;

import com.paranietharan.byteblog.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimitService rateLimitService;
    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        RateLimitPolicy policy = policyFor(request);
        if (policy == null || !securityProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthRateLimitService.RateLimitDecision decision = rateLimitService.consume(
                policy.scope(),
                request.getRemoteAddr(),
                policy.limit(),
                policy.windowSeconds()
        );
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Authentication request limit exceeded\"}");
    }

    private RateLimitPolicy policyFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        SecurityProperties.RateLimit limits = securityProperties.getRateLimit();
        if (HttpMethod.POST.matches(request.getMethod()) && path.endsWith("/auth/login")) {
            return new RateLimitPolicy("login", limits.getLoginLimit(), limits.getLoginWindowSeconds());
        }
        if (HttpMethod.POST.matches(request.getMethod()) && path.endsWith("/auth/register")) {
            return new RateLimitPolicy("registration", limits.getRegistrationLimit(), limits.getRegistrationWindowSeconds());
        }
        if (HttpMethod.POST.matches(request.getMethod()) && path.endsWith("/auth/refresh-token")) {
            return new RateLimitPolicy("refresh", limits.getRefreshLimit(), limits.getRefreshWindowSeconds());
        }
        if (HttpMethod.GET.matches(request.getMethod())
                && (path.endsWith("/auth/verify-email") || path.endsWith("/users/email/verify-change"))) {
            return new RateLimitPolicy("verification", limits.getVerificationLimit(), limits.getVerificationWindowSeconds());
        }
        return null;
    }

    private record RateLimitPolicy(String scope, int limit, int windowSeconds) {
    }
}
