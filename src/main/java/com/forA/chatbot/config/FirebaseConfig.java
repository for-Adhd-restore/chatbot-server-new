package com.forA.chatbot.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

@Configuration
public class FirebaseConfig {

  // 1. 운영용: ECS 환경변수에서 주입받을 Base64 문자열
  @Value("${firebase.key.base64:#{null}}")
  private String firebaseKeyBase64;

  // 2. 개발용: resources 폴더에 있는 파일명
  @Value("${firebase.key.path:mori-firebase-key.json}")
  private String firebaseKeyPath;

  @PostConstruct
  public void init() {
    try {
      // 이미 초기화되었는지 확인 (중복 초기화 방지)
      List<FirebaseApp> apps = FirebaseApp.getApps();
      if (apps != null && !apps.isEmpty()) {
        for (FirebaseApp app : apps) {
          if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
            return; // 이미 있으면 초기화 건너뜀
          }
        }
      }

      InputStream serviceAccountStream;

      // Base64 키가 있으면(운영) 그걸 쓰고, 없으면(개발) 파일을 찾는다.
      if (StringUtils.hasText(firebaseKeyBase64)) {
        // A. 운영 환경: Base64 문자열 -> InputStream 변환
        byte[] decodedBytes = Base64.getDecoder().decode(firebaseKeyBase64);
        serviceAccountStream = new ByteArrayInputStream(decodedBytes);
      } else {
        // B. 개발 환경: JAR 내부 리소스(ClassPath)에서 파일 읽기
        // (FileInputStream 대신 ClassPathResource를 써야 JAR에서도 안전함)
        ClassPathResource resource = new ClassPathResource(firebaseKeyPath);
        if (!resource.exists()) {
          throw new IOException("Firebase 키 파일을 찾을 수 없습니다: " + firebaseKeyPath);
        }
        serviceAccountStream = resource.getInputStream();
      }

      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
          .build();

      FirebaseApp.initializeApp(options);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Firebase 초기화 실패", e);
    }
  }
}