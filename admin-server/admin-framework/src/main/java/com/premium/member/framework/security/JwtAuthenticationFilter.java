package com.premium.member.framework.security;

import java.io.IOException;
import java.util.List;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.framework.cache.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解析 Bearer access token，写入 SecurityContext；无效 token 交给入口点返回 401。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService tokenService;
    private final TokenStore tokenStore;

    public JwtAuthenticationFilter(JwtTokenService tokenService, TokenStore tokenStore) {
        this.tokenService = tokenService;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && !header.isBlank()) {
            try {
                AccessClaims claims = tokenService.parseAccessToken(header.substring(7));
                if (!tokenStore.isAccessTokenRevoked(claims.jti())
                        && !tokenStore.isUserAccessRevoked(claims.user().userId(), claims.issuedAt())) {
                    List<SimpleGrantedAuthority> authorities = claims.permissions().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.user(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (BusinessException ex) {
                SecurityContextHolder.clearContext();
            } catch (RuntimeException ex) {
                log.warn("Token revocation check failed, reject request", ex);
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
