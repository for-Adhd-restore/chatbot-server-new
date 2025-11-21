package com.forA.chatbot.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppleClientSecretGenerator {

  @Value("${apple.team-id}")
  private String teamId;

  @Value("${apple.key-id}")
  private String keyId;

  @Value("${apple.client-id}")
  private String clientId;

  // 1. 운영용: Base64로 인코딩된 키 값 (ECS 환경변수)
  @Value("${apple.key.base64:#{null}}")
  private String keyBase64;

  // 2. 개발용: 키 파일 경로 (로컬 파일)
  @Value("${apple.key.path:AuthKey.p8}")
  private String keyPath;

  public String generate() throws IOException {
    Reader reader;

    // [로직] Base64 키가 있으면(운영) 그걸 쓰고, 없으면(개발) 파일을 읽는다.
    if (StringUtils.hasText(keyBase64)) {
      // A. 운영 환경: Base64 문자열 -> 디코딩 -> StringReader
      // (.p8 파일 전체를 Base64로 인코딩했다고 가정)
      byte[] decodedBytes = Base64.getDecoder().decode(keyBase64);
      String pemString = new String(decodedBytes);
      reader = new StringReader(pemString);
    } else {
      // B. 개발 환경: ClassPathResource -> InputStream
      // (중요) getFile() 대신 getInputStream()을 써야 JAR 안에서도 읽을 수 있음
      ClassPathResource resource = new ClassPathResource(keyPath.replace("classpath:", ""));
      if (!resource.exists()) {
        throw new IOException("Apple Key File not found: " + keyPath);
      }
      reader = new InputStreamReader(resource.getInputStream());
    }

    try (PemReader pemReader = new PemReader(reader)) {
      PemObject pemObject = pemReader.readPemObject();
      if (pemObject == null) {
        throw new RuntimeException("Apple Key 내용이 유효하지 않습니다 (PEM 파싱 실패).");
      }

      byte[] content = pemObject.getContent();
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(content);

      KeyFactory kf = KeyFactory.getInstance("EC");
      PrivateKey privateKey = kf.generatePrivate(spec);

      Map<String, Object> headerParams = new HashMap<>();
      headerParams.put("kid", keyId);
      headerParams.put("alg", "ES256");

      return Jwts.builder()
          .setHeaderParams(headerParams)
          .setIssuer(teamId)
          .setIssuedAt(new Date(System.currentTimeMillis()))
          .setExpiration(new Date(System.currentTimeMillis() + (1000 * 60 * 5))) // 5분 유효
          .setAudience("https://appleid.apple.com")
          .setSubject(clientId)
          .signWith(SignatureAlgorithm.ES256, privateKey)
          .compact();
    } catch (Exception e) {
      throw new RuntimeException("Apple Client Secret 생성 중 오류 발생", e);
    }
  }
}