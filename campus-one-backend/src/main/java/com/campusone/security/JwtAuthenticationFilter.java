package com.campusone.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long blacklistedCount = tokenBlacklistMapper.selectCount(
                    new LambdaQueryWrapper<TokenBlacklist>()
                        .eq(TokenBlacklist::getToken, token));
                if (blacklistedCount > 0) {
                    if ("/api/v1/auth/logout".equals(request.getRequestURI())) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token已失效\",\"success\":false}");
                    return;
                }

                if (jwtUtil.validateToken(token)) {
                    String type = jwtUtil.getTypeFromToken(token);
                    if (!"refresh".equals(type)) {
                        Long userId = jwtUtil.getUserId(token);
                        String username = jwtUtil.getUsername(token);
                        String role = jwtUtil.getRole(token);

                        SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(username);
                        if (user != null && user.isEnabled() && userId.equals(user.getUserId())) {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(request, response);
    }
}
