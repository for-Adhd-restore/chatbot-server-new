package com.forA.chatbot.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse { // 애플 로그인 성공 후 클라이언트에 반환하는 응답
    private String accessToken;
    private String refreshToken;
    private String tokenType; // Bearer
    private Long expiresIn; // token 만료 시간
    private Long userId;
    private boolean isNewUser;
}

