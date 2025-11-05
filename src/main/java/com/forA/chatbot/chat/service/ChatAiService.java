package com.forA.chatbot.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import java.time.LocalDate;
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
  private final ObjectMapper objectMapper;

  /**
   * /generate-response (공감 및 추천 허용)
   * SITUATION_INPUT 단계에서 호출
   * 사용자의 상황, 감정, 프로필을 기반으로 공감 메시지 생성
   */
  public String generateEmpathyResponse(String userSituation, Set<EmotionType> emotions, User user) {
    String emotionsString = emotions.stream().map(EmotionType::getName).collect(Collectors.joining(", "));
    String jobsString = user.getJobs().stream().map(JobType::getName).collect(Collectors.joining(", "));
    String disordersString = user.getDisorders().stream().map(DisorderType::getName).collect(Collectors.joining(", "));

    int age = LocalDate.now().getYear() - user.getBirthYear() + 1;
    String gender = user.getGender().name();

    log.info("GPT 공감 문장 생성 요청. 상황: {}, 감정: {}, 프로필: [{}, {}, {}, {}]",
        userSituation, emotionsString, gender, age, jobsString, disordersString);

    ChatClient chatClient = chatClientBuilder.build();

    String promptMessage = """
        당신은 사용자의 마음을 깊이 공감해주는 AI 상담 친구 '모리'입니다.
        사용자가 방금 자신의 힘든 상황과 감정을 털어놓았습니다.
        사용자의 프로필과 상황을 모두 고려하여, 사용자의 감정에 깊이 공감하는 문장을 딱 한 문장으로 생성해주세요.
        이 문장은 이후에 스킬 추천 제안 질문으로 이어질 것입니다.
        
        [사용자 프로필]
        - 성별: {gender}
        - 나이: {age}세
        - 직업: {jobs}
        - 겪는 어려움(질환): {disorders}

        [사용자 상황]
        {situation}

        [사용자 감정]
        {emotions}

        [규칙]
        1. 사용자의 상황과 감정을 직접적으로 언급하며 공감해주세요. (예: "~한 상황에서 ~한 감정을 느끼시는군요.")
        2. 사용자의 감정이 자연스럽거나 그럴 수 있다는 점을 부드럽게 언급해주세요.
        3. 응답은 반드시 한 문장으로 완성하고, 추가적인 인사나 말을 붙이지 마세요.
        4. 사용자의 프로필(특히 직업, 질환)을 자연스럽게 엮어 공감하면 좋습니다. (예: "ADHD를 겪고 계셔서 시간 관리가 더 힘드셨겠어요.")
        """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "gender", gender,
        "age", age,
        "jobs", jobsString,
        "disorders", disordersString,
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
  /**
   * /skill-description (행동 설명)
   * ACTION_PROPOSE 단계에서 호출
   * 사용자가 선택한 스킬에 대해, 상황/감정에 맞는 공감 + 스킬 설명을 결합한 메시지 생성
   */
  public String generateSkillDescription(String userSituation, Set<EmotionType> emotions, BehavioralSkill selectedSkill, User user) {
    String emotionString = emotions.stream().map(EmotionType::getName).collect(Collectors.joining(", "));
    String skillJsonContext;
    try {
      skillJsonContext = objectMapper.writeValueAsString(selectedSkill);
    } catch (JsonProcessingException e) {
      log.error("Skill JSON 직렬화 실패", e);
      skillJsonContext = selectedSkill.description();
    }
    int age = LocalDate.now().getYear() - user.getBirthYear() + 1;
    String gender = user.getGender().name();

    log.info("GPT 스킬 맞춤 설명 생성 요청. 스킬: {}", selectedSkill.chunk_id());

    ChatClient chatClient = chatClientBuilder.build();
    String promptMessage = """
            당신은 AI 상담 친구 '모리'입니다.
            사용자가 [상황]에서 [감정]을 느끼고 있으며, 이 감정을 다루기 위해 [스킬]을 선택했습니다.
            사용자의 [상황]과 [감정]에 먼저 1~2문장으로 깊이 공감해주세요.
            그런 다음, 이 스킬이 왜 도움이 되는지 [스킬 상세설명]을 활용하여 자연스럽게 설명하는 메시지를 생성해주세요.
            
            [사용자 프로필]
            - 성별: {gender}
            - 나이: {age}세

            [사용자 상황]
            {situation}

            [사용자 감정]
            {emotions}

            [선택한 스킬 정보]
            - 이름: {skillName}
            - 설명: {description}

            [규칙]
            1. 먼저 1~2문장으로 사용자의 상황과 감정에 깊이 공감합니다. (예: "시험 준비를 열심히 했는데... 정말 속상하셨겠어요.")
            2. 그 다음, [선택한 스킬 정보]의 "description" 필드 내용을 자연스럽게 풀어 설명합니다. (예: "이럴 땐... {description} ... 해보는 건 어때요?")
            3. 전체 응답은 부드럽고 따뜻한 '모리'의 말투여야 합니다. (예: ~했군요, ~어때요?, ~거예요)
            4. 스킬의 "description" 내용을 그대로 복사하지 말고, 공감 문장과 자연스럽게 이어지도록 다듬어주세요.
            """;
    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "gender", gender,
        "age", age,
        "situation", userSituation,
        "emotions", emotionString,
        "skillName", selectedSkill.skill_name(),
        "description", selectedSkill.description()
    ));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    Generation generation = response.getResult();
    if (generation != null && generation.getOutput() != null) {
      return generation.getOutput().getText();
    }

    log.error("AI 스킬 맞춤 설명 생성 실패. 기본 설명을 반환합니다.");
    return selectedSkill.description();
  }
  /**
   * /self-soothing-messages (혼자 진정하고 싶을 때)
   * ACTION_OFFER 단계에서 'NO_PROPOSE' 선택 시 호출
   */
  public String generateSelfSoothingMessages(String userSituation, Set<EmotionType> emotions) {
    String emotionString = emotions.stream()
        .map(EmotionType::getName)
        .collect(Collectors.joining(", "));

    ChatClient chatClient = chatClientBuilder.build();
    String promptMessage = """
            당신은 사용자의 마음을 깊이 공감해주는 AI 상담 친구 '모리'입니다.
            사용자가 [상황]으로 인해 [감정]을 느끼고 있으며, '혼자 진정하고 싶다'고 말했습니다.
            
            사용자의 상황과 감정을 따뜻하게 수용하고 공감해주는 위로의 말을 딱 한 문장으로 생성해주세요.
            사용자의 감정을 충분히 이해하고 있으며, 그런 감정이 자연스럽다는 것을 강조해주세요.
            
            [사용자 상황]
            {situation}
            
            [사용자 감정]
            {emotions}
            
            [규칙]
            1. 절대 해결책을 제시하지 마세요.
            2. "힘들었겠어요", "~하셨군요", "~이해가 돼요" 처럼 사용자의 감정과 상황을 그대로 인정해주는 말을 하세요.
            3. 응답은 반드시 세 문장 문장 이내로 완성하고, 추가적인 인사나 말을 붙이지 마세요.
            """;
    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotionString
    ));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    return response.getResult().getOutput().getText();
  }
  /**
   * /action-skipped (행동을 수행하지 않음)
   * SKILL_CONFIRM 단계에서 'SKIP' 선택 시 호출
   */
  public String generateActionSkipped(String userSituation, Set<EmotionType> emotions, BehavioralSkill skippedSkill) {
    String emotionsString = emotions.stream().map(EmotionType::getName).collect(Collectors.joining(", "));
    log.info("GPT 행동 스킵 메시지 생성 요청. 스킬: {}", skippedSkill.chunk_id());

    ChatClient chatClient = chatClientBuilder.build();
    String promptMessage = """
            당신은 20대 상담 친구 '모리'입니다.
            사용자가 [상황]에서 [감정]을 느꼈고, [스킬]을 추천받았으나 방금 '아니, 안 하고 왔어'라고 답했습니다.
            
            사용자를 비난하지 않고, 괜찮다고 다독여주는 따뜻한 메시지를 생성해주세요.
            그리고 원래 스킬과 관련있지만 훨씬 더 간단한, 아주 작은 대안 행동을 1가지만 제안해주세요.
            
            [사용자 상황]
            {situation}
            
            [사용자 감정]
            {emotions}
            
            [스킵한 스킬]
            - 이름: {skillName}
            - 설명: {skillDescription}
            
            [규칙]
            1. "괜찮아요, 그럴 수 있어요" 처럼 사용자를 안심시키는 말로 시작합니다.
            2. "지금 이렇게 모리랑 이야기해주는 것만으로도 정말 고마워요."와 같이 사용자의 현재 행동을 긍정합니다.
            3. 마지막으로, 스킵한 스킬과 연관된 매우 간단한 대안을 1가지만 제안합니다. (예: "혹시 지금 깊게 숨 한 번 쉬어보는 건 어떨까요?")
            4. 전체 응답은 2~3문장으로 구성합니다.
            """;
    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of(
        "situation", userSituation,
        "emotions", emotionsString,
        "skillName", skippedSkill.skill_name(),
        "skillDescription", skippedSkill.description()
    ));

    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    Generation generation = response.getResult();

    if (generation != null && generation.getOutput() != null) {
      return generation.getOutput().getText();
    }

    log.error("AI 행동 스킵 메시지 생성 실패. 기본 메시지를 반환합니다.");
    return "괜찮아요, 그럴 수 있죠. 지금은 그럴 마음이 안 생길 수도 있어요. 모리랑 다시 이야기해줘서 고마워요. 혹시 지금 딱 한 번만 깊게 숨을 쉬어보는 건 어때요?";
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

  /**
   * /recommend-skills (행동 추천)
   * ACTION_OFFER 단계에서 'YES_PROPOSE' 선택 시 호출
   * 4개의 chunk_id 리스트를 반환
   */
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
    List<String> recommendedChunkIds = Collections.emptyList();

    Generation generation = response.getResult();
    if (generation != null && generation.getOutput() != null) {
      String rawResponse = generation.getOutput().getText().trim();
      log.info("gpt 응답 skill IDs: {}", rawResponse);

      recommendedChunkIds = Arrays.stream(rawResponse.split(","))
          .map(String::trim) // 각 ID 앞뒤 공백 제거
          .filter(id -> id.matches("distress-\\d{3}")) // 유효한 형식인지 확인
          .distinct() // 중복 제거
          .limit(4) // 최대 4개만 사용
          .collect(Collectors.toList());

      if (recommendedChunkIds.size() < 4) {
        log.warn("GPT 4개 chunk_ids 반환 ({} found).", recommendedChunkIds.size());
        List<String> defaultIds = List.of("distress-001", "distress-002", "distress-003", "distress-005");
        for (String defaultId : defaultIds) {
          if (recommendedChunkIds.size() >= 4) break;
          if (!recommendedChunkIds.contains(defaultId)) {
            recommendedChunkIds.add(defaultId);
          }
        }
      }
    } else {
      log.error("스킬 추천 AI 응답에서 유효한 Generation 또는 Output을 얻지 못했습니다. 기본 ID 리스트를 반환합니다.");
      recommendedChunkIds = List.of("distress-001", "distress-002", "distress-003", "distress-005");
    }

    log.info("최종 추천된 스킬 chunk_id 리스트 ({}개): {}", recommendedChunkIds.size(), recommendedChunkIds);
    return recommendedChunkIds;
  }
  /**
   * 상세 행동 4가지 제안 (ChatService에서 계속 사용)
   * SKILL_SELECT 단계의 버튼 생성을 위해 호출
   */
  public List<String> generateDetailedSkillSteps(BehavioralSkill skill) {
    log.info("AI 상세 행동 4가지 제안 요청: {}", skill.chunk_id());
    ChatClient chatClient = chatClientBuilder.build();

    String skillJsonContext;
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      skillJsonContext = objectMapper.writeValueAsString(skill);
    } catch (JsonProcessingException e) {
      log.error("Skill JSON 직렬화 실패", e);
      skillJsonContext = skill.description();
    }

    String promptMessage = """
            당신은 DBT(변증법적 행동 치료) 전문가입니다.
            사용자가 다음 JSON 정보에 해당하는 행동을 선택했습니다:
            [컨텍스트]
            {skillContext}
            
            이 행동을 실제로 실천할 수 있는 4가지의 구체적이고 간단한 '상세 행동'을 제안해주세요.
            
            [규칙]
            1. 응답은 반드시 4개의 짧은 행동(예: "거울 보고 웃기") 리스트여야 합니다.
            2. 4개의 항목을 콤마(,)로만 구분된 하나의 문자열로 반환하세요.
            3. (예시): 거울 보고 칭찬하기,내가 잘한 일 3가지 적기,따뜻한 물로 샤워하기,좋아하는 음악 1곡 듣기
            4. 절대 다른 설명이나 번호(1., 2.)를 붙이지 마세요.
            """;

    PromptTemplate promptTemplate = new PromptTemplate(promptMessage);
    Prompt prompt = promptTemplate.create(Map.of("skillContext", skillJsonContext));

    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    Generation generation = response.getResult();

    if (generation != null && generation.getOutput() != null) {
      String rawResponse = generation.getOutput().getText().trim();
      log.info("AI 상세 행동 응답 (Raw): {}", rawResponse);

      return Arrays.stream(rawResponse.split(","))
          .map(String::trim)
          .limit(4)
          .collect(Collectors.toList());
    }

    log.error("AI 상세 행동 생성 실패. 임시값을 반환합니다.");
    return List.of(
        "상세 행동 1 (AI 생성 실패)",
        "상세 행동 2 (AI 생성 실패)",
        "상세 행동 3 (AI 생성 실패)",
        "상세 행동 4 (AI 생성 실패)"
    );
  }
}
