package com.forA.chatbot.auth.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;


@Getter
public class CustomUserDetails implements UserDetails {

  private final Long userId;

  public CustomUserDetails(Long userId) {
    this.userId = userId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList(); // 권한은 따로 안 쓰면 빈 리스트
  }

  @Override
  public String getPassword() {
    return null; // 비밀번호 인증 안 쓰므로 null
  }

  @Override
  public String getUsername() {
    return String.valueOf(userId);
  }

  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }
}