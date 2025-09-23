package com.forA.chatbot.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info("🔥 JwtAuthenticationFilter 진입 - URI: {}", request.getRequestURI());
    String token = jwtUtil.extractTokenFromRequest(request);
    log.info("🔥 추출된 토큰: {}", token);

    if (token != null && jwtUtil.validateToken(token)) {
      Long userId = jwtUtil.getUserIdFromToken(token);
      log.info("토큰에서 꺼낸 userId: {}", userId);
      Authentication authentication =
          new UsernamePasswordAuthenticationToken(
              new CustomUserDetails(userId), null, Collections.emptyList());

      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.info("JWT 인증 성공 - userId: {}", userId);
    }

    filterChain.doFilter(request, response);
    log.info("Authentication 객체: {}", SecurityContextHolder.getContext().getAuthentication());
  }
}
