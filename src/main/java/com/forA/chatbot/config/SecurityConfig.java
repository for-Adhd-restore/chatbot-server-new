package com.forA.chatbot.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.forA.chatbot.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(withDefaults()) // WebConfig에서 설정한 CORS 정책을 따르도록 설정
        .authorizeHttpRequests(
            auth ->
                auth
                    // 인증 불필요 (permitAll)
                    .requestMatchers(
                        "/",
                        "/test/**",
                        "/api/v1/auth/**", // 로그인, 애플 로그인, 임시 로그인 포함
                        "/api/v1/subscription/notification", // Apple Webhook 수신 경로
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**")
                    .permitAll()

                    // 그 외 모든 요청은 인증 필요
                    .anyRequest()
                    .authenticated())
        // JWT 인증 필터 등록
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
