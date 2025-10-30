package com.forA.chatbot.chat.service;

import com.forA.chatbot.chat.domain.enums.EmotionType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAiService {
  private final ChatClient.Builder chatClientBuilder;

  public String generateEmpathySentence(String userSituation, Set<EmotionType> emotions) { // 메서드 이름 및 로직 변경
    String emotionsString = emotions.stream().map(EmotionType::getName).collect(Collectors.joining(", "));
    log.info("GPT 공감 문장 생성 요청. 상황: {}, 감정: {}", userSituation, emotionsString);
    ChatClient chatClient = chatClientBuilder.build();

    String promptMessage = """
                당신은 사용자의 마음을 깊이 공감해주는 20대 여성 AI 상담 친구 '모리'입니다.
                사용자가 방금 자신의 힘든 상황과 감정을 털어놓았습니다.

                사용자의 상황과 감정에 깊이 공감하는 문장을 딱 한 문장으로 생성해주세요.
                이 문장은 이후에 스킬 추천 제안 질문으로 이어질 것입니다.
                사용자의 감정을 인정하고 그것이 자연스럽다는 뉘앙스를 포함해주세요.

                [사용자 상황]
                {situation}

                [사용자 감정]
                {emotions}

                [규칙]
                1. 사용자의 상황과 감정을 직접적으로 언급하며 공감해주세요. (예: "~한 상황에서 ~한 감정을 느끼시는군요.")
                2. 해당 감정이 자연스럽거나 그럴 수 있다는 점을 부드럽게 언급해주세요. (예: "그런 기분이 드는 건 당연해요.", "충분히 그럴 수 있어요.")
                3. 응답은 반드시 한 문장으로 완성하고, 추가적인 인사나 말을 붙이지 마세요.
                4. 너무 길지 않게 작성해주세요.
                """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotionsString
    ));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

    Generation generation = response.getResult();
    if (generation != null && generation.getOutput() != null) {
      return generation.getOutput().getText();
    } else {
      log.error("AI 응답(공감 문장)에서 유효한 Generation 또는 Output을 얻지 못했습니다.");
      return "그 상황에서 마음이 많이 복잡하고 힘들었겠어요.";
    }
  }
  public String generateProposalGoalPhrase(String userSituation, Set<EmotionType> emotions) {
    String emotionsString = emotions.stream().map(EmotionType::getName).collect(Collectors.joining(", "));
    log.info("GPT 제안 목표 구문 생성 요청. 상황: {}, 감정: {}", userSituation, emotionsString);
    ChatClient chatClient = chatClientBuilder.build();

    String promptMessage = """
                당신은 DBT(변증법적 행동 치료) 전문가입니다.
                사용자의 상황과 감정을 고려할 때, 다음에 추천할 행동 지침(스킬)들이 어떤 **목표**를 가지는지 설명하는 **짧은 한국어 구문**을 생성해주세요.
                이 구문은 "모리가 [닉네임]님을 위해 [여기에 삽입될 구문] 도움이 될 수 있는 방법을 추천 드려도 될까요?" 라는 질문에 사용됩니다.

                [사용자 상황]
                {situation}

                [사용자 감정]
                {emotions}

                [규칙]
                1. 상황과 감정을 바탕으로, 추천될 스킬들이 궁극적으로 사용자에게 어떤 도움을 줄 수 있는지 핵심 목표를 요약하세요.
                2. 응답은 반드시 **"~하는", "~도록 돕는"** 과 같은 형태의 **짧은 구문**(5~10자 내외)이어야 합니다. (예: "마음을 차분하게 가라앉히는", "복잡한 생각을 잠시 멈추는", "기분을 전환하는")
                3. 오직 생성된 구문만 반환하고, 다른 설명이나 문장은 절대 포함하지 마세요.
                """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotionsString
    ));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

    Generation generation = response.getResult();
    if (generation != null && generation.getOutput() != null) {
      String phrase = generation.getOutput().getText().trim();
      // 간단한 후처리 추가 (혹시 모를 괄호 제거)
      phrase = phrase.replaceAll("[()]", "");
      if (phrase.length() > 20 || phrase.endsWith("?") || phrase.endsWith(".")) { // 길이 제한 약간 늘림
        log.warn("Generated proposal goal phrase is too long or has wrong format: '{}'. Using default.", phrase);
        return "마음을 진정시키는";
      }
      return phrase;
    } else {
      log.error("AI 응답(제안 목표 구문)에서 유효한 Generation 또는 Output을 얻지 못했습니다.");
      return "마음을 진정시키는";
    }
  }
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

  public List<String> recommendSkillChunkId(String userSituation, String emotions, String allSkillsJson) {
    log.info("상황에 맞는 스킬 4개 추천 요청: {}, 감정: {}", userSituation, emotions);
    ChatClient chatClient = chatClientBuilder.build();
    String promptMessage = """
            당신은 DBT(변증법적 행동 치료) 전문가입니다.
            사용자의 현재 상황과 감정을 듣고,
            제공된 [행동 지침 목록(JSON)] 중에서 가장 도움이 될 것 같은 행동 지침 **4개**를 추천해야 합니다.

            [사용자 상황]
            {situation}

            [사용자 감정]
            {emotions}

            [행동 지침 목록(JSON)]
            {skills}

            [규칙]
            1. 사용자의 [상황]과 [감정]을 [행동 지침 목록]의 'situation_tags'와 'emotion_tags'와 비교하여 가장 적절한 'chunk_id' **4개**를 선택하세요.
            2. 관련성이 높은 순서대로 정렬하세요.
            3. 당신의 응답은 반드시 선택된 'chunk_id' 값 4개를 **콤마(,)로 구분한 문자열**이어야 합니다. (예: "distress-005,distress-001,distress-014,distress-026")
            4. 어떠한 설명이나 추가 텍스트도 포함하지 마세요. 오직 'chunk_id' 4개를 콤마로 구분한 문자열만 반환하세요.
            """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotions,
        "skills", allSkillsJson
    ));

    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    List<String> recommendedChunkIds = Collections.emptyList(); // 기본 빈 리스트

    Generation generation = response.getResult();
    if (generation != null && generation.getOutput() != null) {
      String rawResponse = generation.getOutput().getText().trim();
      log.info("GPT raw response for skill IDs: {}", rawResponse);

      // 응답 파싱 및 검증 로직 강화
      // 1. 콤마로 분리
      recommendedChunkIds = Arrays.stream(rawResponse.split(","))
          .map(String::trim) // 각 ID 앞뒤 공백 제거
          .filter(id -> id.matches("distress-\\d{3}")) // 유효한 형식인지 확인
          .distinct() // 중복 제거
          .limit(4) // 최대 4개만 사용
          .collect(Collectors.toList());

      // 2. 개수 확인 및 부족하면 기본값 추가 (혹시 모를 경우 대비)
      if (recommendedChunkIds.size() < 4) {
        log.warn("GPT returned fewer than 4 valid chunk_ids ({} found). Will add default IDs.", recommendedChunkIds.size());
        List<String> defaultIds = List.of("distress-001", "distress-002", "distress-003", "distress-005"); // 예시 기본값
        for (String defaultId : defaultIds) {
          if (recommendedChunkIds.size() >= 4) break; // 4개 채워지면 중단
          if (!recommendedChunkIds.contains(defaultId)) { // 중복 방지
            recommendedChunkIds.add(defaultId);
          }
        }
      }
    } else {
      log.error("스킬 추천 AI 응답에서 유효한 Generation 또는 Output을 얻지 못했습니다. 기본 ID 리스트를 반환합니다.");
      recommendedChunkIds = List.of("distress-001", "distress-002", "distress-003", "distress-005"); // Fallback
    }

    log.info("최종 추천된 스킬 chunk_id 리스트 ({}개): {}", recommendedChunkIds.size(), recommendedChunkIds);
    return recommendedChunkIds;
  }
}
