package com.forA.chatbot.chat.service;

import com.forA.chatbot.chat.domain.enums.EmotionType;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAiService {
  private final ChatClient.Builder chatClientBuilder;
  /**
   * @param userSituation 유저가 입력한 상황 텍스트
   * @param emotions 유저가 선택한 감정
   * @return GPT가 생성한 1-2 문장의 위로 메시지
   */
  public String generateSituationalComfort(String userSituation, Set<EmotionType> emotions) {
    String emotionsString = emotions.stream()
        .map(EmotionType::getName) // Or use Enum::name depending on what GPT expects
        .collect(Collectors.joining(","));

    log.info("GPT 위로 메시지 생성 요청. 상황: {}", userSituation);
    ChatClient chatClient = chatClientBuilder.build();
    String promptMessage = """
            당신은 사용자의 마음을 깊이 공감해주는 20대 여성 AI 상담 친구 '모리'입니다.
            사용자가 방금 자신의 힘든 상황과 감정을 털어놓았습니다.
            사용자가 '혼자 진정하고 싶다'고 말한 상황입니다.
            
            사용자의 상황과 감정을 따뜻하게 수용하고 공감해주는 위로의 말을 딱 한 문장으로 생성해주세요.
            
            [사용자 상황]
            {situation}
            
            [사용자 감정]
            {emotions}
            
            [규칙]
            1. 절대 해결책을 제시하지 마세요.
            2. "힘들었겠어요", "~하셨군요" 처럼 사용자의 감정과 상황을 그대로 인정해주는 말을 하세요.
            3. 응답은 반드시 한 문장으로 완성하고, 추가적인 인사나 말을 붙이지 마세요.
            """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotionsString
    ));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    return response.getResult().getOutput().getText();
  }
  /**
   * 사용자의 상황/감정과 가장 일치하는 행동 지침(skill)의 'chunk_id'를 추천
   * @param userSituation 사용자가 입력한 상황 텍스트
   * @param emotions 사용자가 선택한 감정 (e.g., "화남,불안함")
   * @param allSkillsJson `behavioral-skills.json`의 전체 내용
   * @return 추천하는 `chunk_id` (e.g., "distress-001")
   */
  public String recommendSkillChunkId(String userSituation, String emotions, String allSkillsJson) {
    log.info("Recommending skill for situation: {}, emotions: {}", userSituation, emotions);
    ChatClient chatClient = chatClientBuilder.build();

    String promptMessage = """
            당신은 DBT(변증법적 행동 치료) 전문가입니다.
            사용자의 현재 상황과 감정을 듣고,
            제공된 [행동 지침 목록(JSON)] 중에서 가장 도움이 될 것 같은 행동 지침 "하나"를 추천해야 합니다.
            
            [사용자 상황]
            {situation}
            
            [사용자 감정]
            {emotions}
            
            [행동 지침 목록(JSON)]
            {skills}
            
            [규칙]
            1. 사용자의 [상황]과 [감정]을 [행동 지침 목록]의 'situation_tags'와 'emotion_tags'와 비교하여 가장 적절한 'chunk_id' "하나"만 선택하세요.
            2. 당신의 응답은 반드시 선택된 'chunk_id' 값 하나여야 합니다. (예: "distress-005")
            3. 어떠한 설명이나 추가 텍스트도 포함하지 마세요. 오직 'chunk_id' 문자열만 반환하세요.
            """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotions,
        "skills", allSkillsJson
    ));

    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    String recommendedChunkId = response.getResult().getOutput().getText().trim();

    // GPT가 혹시 모를 추가 텍스트를 붙였을 경우 파싱 (예: "추천 ID는 distress-001입니다." -> "distress-001")
    if (recommendedChunkId.contains("\"")) {
      recommendedChunkId = recommendedChunkId.replaceAll("\"", "");
    }
    if (recommendedChunkId.contains("distress-")) {
      int index = recommendedChunkId.indexOf("distress-");
      if (index + "distress-000".length() <= recommendedChunkId.length()) {
        return recommendedChunkId.substring(index, index + "distress-000".length());
      }
    }

    log.info("Recommended skill chunk_id: {}", recommendedChunkId);
    // chunk_id 형식(distress-XXX)이 아닌 경우, 기본값(001)을 반환하여 오류 방지
    if (!recommendedChunkId.matches("distress-\\d{3}")) {
      log.warn("GPT response was not a valid chunk_id, returning default 'distress-001'. GPT Response: {}", recommendedChunkId);
      return "distress-001";
    }
    return recommendedChunkId;
  }
}
