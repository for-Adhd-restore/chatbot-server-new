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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String token = jwtUtil.extractTokenFromRequest(request);

    if (token != null && jwtUtil.validateToken(token)) {
      Long userId = jwtUtil.getUserIdFromToken(token);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug("JWT 인증 성공 - userId: {}", userId);
    }

    filterChain.doFilter(request, response);
  }
}
