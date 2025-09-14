package com.forA.chatbot.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppleClientSecretGenerator { // apple 공식 요구사항에 맞게 ClientSecret(=JWT) 생성

  @Value("${apple.team-id}")
  private String teamId;
  @Value("${apple.key-id}")
  private String keyId;
  @Value("${apple.client-id}")
  private String clientId;
  @Value("${apple.key.path}")
  private String keyPath;

  public String generate() throws IOException {
    ClassPathResource resource = new ClassPathResource(keyPath.replace("classpath:", ""));

    try (PemReader pemReader = new PemReader(new FileReader(resource.getFile()))) {
      PemObject pemObject = pemReader.readPemObject();
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
