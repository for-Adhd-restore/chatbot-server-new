package com.forA.chatbot.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.ChatHandler;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.chat.domain.enums.EmotionType.EmotionState;
import com.forA.chatbot.chat.dto.ChatRequest;
import com.forA.chatbot.chat.dto.ChatResponse;
import com.forA.chatbot.chat.dto.ChatResponse.ChatBotMessage;
import com.forA.chatbot.chat.dto.ChatResponse.ChatMessageDto;
import com.forA.chatbot.chat.dto.ChatResponse.MessageType;
import com.forA.chatbot.chat.repository.ChatMessageRepository;
import com.forA.chatbot.chat.repository.ChatSessionRepository;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

record BehavioralSkill(String chunk_id, String skill_type, List<String> situation_tags,
                       String skill_origin, String skill_name, String description,
                       List<String> emotion_tags) {}

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatSessionRepository chatSessionRepository; // 세션 관리
  private final ChatMessageRepository chatMessageRepository; // 메시지 기록
  private final ChatResponseGenerator responseGenerator;
  private final UserRepository userRepository;
  private final ApplicationContext applicationContext;
  private final ObjectMapper objectMapper;
  private final ChatAiService chatAiService;
  private List<BehavioralSkill> behavioralSkills = Collections.emptyList();

  @PostConstruct
  public void loadBehavioralSkills() {
    try {
      Resource resource = applicationContext.getResource("classpath:behavioral-skills.json");
      InputStream inputStream = resource.getInputStream();
      this.behavioralSkills = objectMapper.readValue(inputStream, new TypeReference<List<BehavioralSkill>>() {});
      log.info("Loaded {} behavioral skills from JSON.", behavioralSkills.size());
    } catch (Exception e) {
      log.error("Failed to load behavioral skills from JSON", e);
      this.behavioralSkills = Collections.emptyList();
    }
  }
  // 3번을 넘긴 후 대화 진행 x
  @Transactional
  public ChatResponse initializeSession(Long userId) {
    log.info("Chat session initialization for userId: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserHandler(ErrorStatus.USER_NOT_FOUND));

    ChatSession session;
    List<ChatMessageDto> history;
    boolean isResuming =  false;

    // 1. 온보딩 (1 ~ 5)을 완료하지 않은 세션이 있는지 확인 (중간 이탈자)
    Optional<ChatSession> activeSessionOpt = chatSessionRepository
        .findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(userId);

    if (activeSessionOpt.isPresent()) {
      // 온보딩(1~5) 중에 나갔다가 다시 들어온 경우
      session = activeSessionOpt.get();
      history = getChatHistory(session.getId());// 기존 대화 기록 로드
      isResuming = true;
      log.info("Resuming existing incomplete session: {}", session.getId());
    } else {
      // 2. 가장 최근 세션을 찾아, 온보딩을 완료했었는지(기존 유저인지) 확인
      Optional<ChatSession> lastSessionOpt = chatSessionRepository.findFirstByUserIdOrderByStartedAtDesc(userId);

      // 사용자가 온보딩을 완료한 적이 있는지 여부
      boolean isUserOnboarded = lastSessionOpt
          .map(ChatSession::getOnboardingCompleted)
          .orElse(false);

      // 3. 시작 단계 결정
      String initialStep = isUserOnboarded ? ChatStep.EMOTION_SELECT.name() : ChatStep.GENDER.name();

      // 4. 새로운 세션 생성
      session = ChatSession.builder()
          .userId(userId)
          .currentStep(initialStep)
          .onboardingCompleted(isUserOnboarded)
          .startedAt(LocalDateTime.now())
          .build();

      session = chatSessionRepository.save(session);
      history = new ArrayList<>(); // 새 세션 시작
      log.info("Starting new session. Onboarded: {}, Initial Step: {}", isUserOnboarded, initialStep);
    }

    // 5. 현재 단계(currentStep)에 맞는 봇 메시지 생성
    // (기존 유저 여부에 따라 6번 멘트가 달라지므로 isUserOnboarded 플래그 전달)
    ChatBotMessage botMessage = responseGenerator.getBotMessageForStep(session.getCurrentStep(), user, session.getOnboardingCompleted());

    // 6. 새 세션인 경우에만 봇의 첫 메시지를 DB에 기록하고, history에도 추가
    if (!isResuming) {
      recordBotMessage(session.getId(), session.getCurrentStep(), botMessage.getContent());
      // 방금 기록한 봇 메시지를 클라이언트에게 바로 보여주기 위해 history에 추가
      history.add(ChatMessageDto.builder()
          .sender("BOT")
          .content(botMessage.getContent())
          .sentAt(LocalDateTime.now())
          .build());
    }
    // 7. 세션 마지막 상호작용 시간 업데이트
    session.setLastInteractionAt(LocalDateTime.now());
    chatSessionRepository.save(session);
    // 8. 최종 응답 반환
    return ChatResponse.builder()
        .sessionId(session.getId())
        .currentStep(session.getCurrentStep())
        .messages(history) // [중간 이탈자]는 기존 기록, [신규/기존]은 봇의 첫 메시지
        .botMessage(botMessage) // 봇이 다음으로 할 말
        .isCompleted(false)
        .onboardingCompleted(session.getOnboardingCompleted())
        .build();
  }
  /**
   * [2. 유저 응답 처리]
   */
  @Transactional
  public ChatResponse handleUserResponse(Long userId, String sessionId, ChatRequest request) {
    // 1. 세션 및 유저 정보 로드
    ChatSession session = chatSessionRepository.findById(sessionId)
        .orElseThrow(() -> new ChatHandler(ErrorStatus.SESSION_NOT_FOUND));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserHandler(ErrorStatus.USER_NOT_FOUND));
    ChatStep currentStep = ChatStep.valueOf(session.getCurrentStep());
    String userResponse = request.getResponseValue();
    // 2. 사용자 응답 메시지 DB에 기록
    recordUserMessage(sessionId, currentStep.name(), userResponse);

    ChatStep nextStep = currentStep;
    ChatBotMessage botMessage; // 봇이 보낼 다음 메시지
    // (이전 단계에서 저장된 임시 데이터 가져오기)
    String selectedEmotionsString = session.getTemporaryData("selectedEmotions");
    Set<EmotionType> selectedEmotions = parseEmotionsFromString(selectedEmotionsString);
    String userSituation = session.getTemporaryData("userSituation");
    // 3. 현재 단계(currentStep)에 따라 로직 분기 (switch)
    try {
      switch (currentStep) {
        case GENDER:
          user.updateGender(Gender.valueOf(userResponse));
          nextStep = ChatStep.BIRTH_YEAR;
          botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          break;
        case BIRTH_YEAR:
          int birthYear = Integer.parseInt(userResponse);
          // TODO : 임시 생년 유효범위 세팅
          if(birthYear < 1900 || birthYear > 2030) {
            throw new UserHandler(ErrorStatus.INVALID_YEAR_OF_BIRTH);
          }
          user.updateBirthYear(birthYear);
          nextStep = ChatStep.JOB_TYPE;
          botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          break;
        case JOB_TYPE: // 3. 직업 응답 처리
          Set<JobType> jobs = parseAndValidateMultiSelect(userResponse, JobType::valueOf, 2, "직업");
          user.updateJobs(jobs);
          nextStep = ChatStep.DISORDER_TYPE;
          botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          break;
        case DISORDER_TYPE:
          Set<DisorderType> disorders = parseAndValidateMultiSelect(userResponse, DisorderType::valueOf, 2, "질환");
          user.updateDisorders(disorders); // User 엔티티에 질환 저장

          if (disorders.stream().anyMatch(d -> d == DisorderType.NONE)) { // '없음' 선택 시
            nextStep = ChatStep.EMOTION_SELECT; // 증상 건너 뛰고 감정 선택으로
            session.setOnboardingCompleted(true); // 온보딩 완료
            botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          } else {
            nextStep = ChatStep.SYMPTOM_TYPE; // 다음 단계: 5번(증상)
            // 5단계 질문(증상 버튼)은 동적으로 생성해야 함
            botMessage = responseGenerator.createSymptomMessage(disorders);
          }
          break;
        case SYMPTOM_TYPE: // 5. 증상 응답 처리 (온보딩 마지막)
          Set<SymptomType> symptoms = parseAndValidateMultiSelect(userResponse, SymptomType::valueOf, Integer.MAX_VALUE, "증상");
          user.updateSymptoms(symptoms);

          nextStep = ChatStep.EMOTION_SELECT; // 다음 단계: 6번(감정)
          session.setOnboardingCompleted(true); // ★ 온보딩 완료
          botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          break;
        case EMOTION_SELECT:
          Set<EmotionType> emotions = parseAndValidateMultiSelect(userResponse, EmotionType::valueOf, 2, "감정");
          // 선택된 감정을 세션에 임시 저장 (SITUATION_INPUT 메시지에 사용)
          String emotionsStringValue = emotions.stream().map(Enum::name).collect(Collectors.joining(","));
          session.setTemporaryData("selectedEmotions", emotionsStringValue);

          // 감정 상태에 따른 분기 처리
          if (isPositiveOrSoSo(emotions)) {
            // 긍정/괜찮음 -> 단순 종료
            nextStep = ChatStep.CHAT_END;
            botMessage = responseGenerator.createPositiveResponseMessage(emotions);
          } else {
            // 부정/중립 -> 상황 질문
            nextStep = ChatStep.SITUATION_INPUT;
            botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, true, emotions);
          }
          break;
        case SITUATION_INPUT:
          userSituation = userResponse;
          // 입력된 상황을 세션에 임시 저장 (GPT에 추후 전달)
          session.setTemporaryData("userSituation", userSituation);
          nextStep = ChatStep.ACTION_PROPOSE; // 다음 단계: 도움 제안
          botMessage = responseGenerator.createActionProposeMessage(user.getNickname());
          break;
        case ACTION_PROPOSE:
          if ("YES_PROPOSE".equals(userResponse)) {
            nextStep = ChatStep.SKILL_SELECT; // (새로운 단계 정의 필요)

            // 1. AI 에게 추천 요청
            String skillJson = convertSkillsToJson();
            List<String> recommendedIds = chatAiService.recommendSkillChunkId(userSituation, selectedEmotionsString, skillJson);
            // 2. GPT가 추천한 ID 리스트로 BehavioralSkill 객체 리스트 생성
            List<BehavioralSkill> recommendedSkills = recommendedIds.stream()
                .map(id -> behavioralSkills.stream()
                    .filter(skill -> skill.chunk_id().equals(id))
                    .findFirst()
                    .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            botMessage = responseGenerator.createSkillSelectMessage(recommendedSkills); // GPT 호출 (나중에 구현)
          } else if ("NO_PROPOSE".equals(userResponse)) {
            nextStep = ChatStep.CHAT_END;
            // 1. AI에게 위로 메시지 생성 요청
            String gptComfortMessage = chatAiService.generateSituationalComfort(userSituation, selectedEmotions);
            // 2. 생성기에게 위로 메시지 전달
            botMessage = responseGenerator.createAloneComfortMessage(user.getNickname(), gptComfortMessage);
          } else {
            throw new ChatHandler(ErrorStatus.INVALID_BUTTON_SELECTION);
          }
          break;
        case CHAT_END:
          log.info("Chat session {} already ended.", sessionId);
          botMessage = responseGenerator.getBotMessageForStep(currentStep.name(), user, true);
          break;
        // TODO : 6.1.3 단계 이후는 나중에 구현
        default:
          log.warn("handleUserResponse: Unhandled step: {}", currentStep);
          throw new IllegalArgumentException("처리할 수 없는 단계입니다.");
      }
    } catch (IllegalArgumentException e) {
      log.warn("Invalid user response: {} for step: {}. Error: {}", userResponse, currentStep, e.getMessage());
      botMessage = ChatBotMessage.builder()
          .content(e.getMessage() + "\n다시 선택해주세요.")
          .type(MessageType.TEXT)
          .build();
    } catch (ChatHandler e) {
      log.warn("Chat handling error: Code={}, Message={}", e.getCode(), e.getMessage());
      botMessage = ChatBotMessage.builder()
          .content(e.getMessage() + "\n다시 시도해주세요.")
          .type(MessageType.TEXT)
          .build();
    }

    // 4. 유저 정보 및 세션 상태 저장
    userRepository.save(user); // 1~5단계에서 변경된 유저 정보(성별, 생년 등)를 DB에 최종 저장
    session.setCurrentStep(nextStep.name());
    session.setLastInteractionAt(LocalDateTime.now());

    // 대화 종료 시 세션에 종료 시간 기록
    if(nextStep == ChatStep.CHAT_END) {
      session.setEndedAt(LocalDateTime.now());
      // 대화 종료 시 임시 데이터 삭제
      session.clearTemporaryData();
    }
    chatSessionRepository.save(session);

    // 5. 봇의 다음 응답 메시지 DB에 기록
    recordBotMessage(sessionId, nextStep.name(), botMessage.getContent());

    // 6. 최종 응답 반환
    return ChatResponse.builder()
        .sessionId(session.getId())
        .currentStep(nextStep.name())
        .botMessage(botMessage)
        .isCompleted(nextStep == ChatStep.CHAT_END)
        .onboardingCompleted(session.getOnboardingCompleted())
        .build();
  }

  private Set<EmotionType> parseEmotionsFromString(String emotionString) {
    if (emotionString == null || emotionString.isEmpty()) {
      return Collections.emptySet();
    }
    return Arrays.stream(emotionString.split(","))
        .map(EmotionType::valueOf)
        .collect(Collectors.toSet());
  }

  private String convertSkillsToJson() {
    try {
      return objectMapper.writeValueAsString(this.behavioralSkills);
    } catch (JsonProcessingException e) {
      log.error("Json 문자열 직렬화 실패", e);
      return "[]"; // 오류 시 빈 배열 반환
    }
  }

  /**
   * 선택한 감정이 '긍정' 또는 '괜찮음'인지 확인
   */
  private boolean isPositiveOrSoSo(Set<EmotionType> emotions) {
    if (emotions.isEmpty()) {
      throw new IllegalArgumentException("감정을 선택해주세요.");
    }
    // "긍정" 감정이거나 "괜찮음(SO_SO)"만 있는지 확인
    return emotions.stream().allMatch(e ->
        e.getState() == EmotionState.POSITIVE || //
            e == EmotionType.SO_SO
    );
  }

  /**
   * 사용자 메시지를 DB에 기록
   */
  private void recordUserMessage(String sessionId, String step, String content) {
    ChatMessage message = ChatMessage.builder()
        .sessionId(sessionId)
        .senderType(ChatMessage.SenderType.USER)
        .chatStep(step)
        .messageContent(content)
        .responseCode(content) // 선택/입력값 원본 저장
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);
  }

  /**
   * 특정 세션의 모든 대화 기록을 불러옵니다.
   */
  private List<ChatMessageDto> getChatHistory(String sessionId) {
    List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
    return messages.stream()
        .map(msg -> ChatMessageDto.builder()
            .sender(msg.getSenderType().name())
            .content(msg.getMessageContent())
            .sentAt(msg.getSentAt())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * 봇의 응답을 MongoDB에 기록
   */
  private void recordBotMessage(String sessionId, String step, String content) {
    ChatMessage message = ChatMessage.builder()
        .sessionId(sessionId)
        .senderType(ChatMessage.SenderType.BOT)
        .chatStep(step)
        .messageContent(content)
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);
  }

  private <T extends Enum<T>> Set<T> parseAndValidateMultiSelect(
      String responseValue,
      java.util.function.Function<String, T> valueOf,
      int maxLimit,
      String entityName
  ) {
    if(responseValue == null || responseValue.isEmpty()) {
      throw new IllegalArgumentException(entityName + "을(를) 선택해주세요.");
    }
    String[] values = responseValue.split(",");
    if (values.length > maxLimit) {
      throw new IllegalArgumentException(String.format("%s은(는) 최대 %d개까지 선택 가능합니다.", entityName, maxLimit));
    }
    return Arrays.stream(values)
        .map(valueOf)
        .collect(Collectors.toSet());
  }

  /**
   * 1. AI가 추천한 Primary Skill ID를 받음
   * 2. 해당 스킬을 리스트의 첫 번째로 추가
   * 3. 감정 태그가 일치하는 다른 스킬들을 찾아 'count' 개수만큼 채움
   */
  private List<BehavioralSkill> findSkills(String primarySkillId, Set<EmotionType> emotions, int count) {
    Optional<BehavioralSkill> primary = behavioralSkills.stream()
        .filter(skill -> skill.chunk_id().equals(primarySkillId))
        .findFirst();

    List<String> emotionNames = emotions.stream().map(EmotionType::getName).collect(Collectors.toList());
    List<BehavioralSkill> others = behavioralSkills.stream()
        .filter(skill -> !skill.chunk_id().equals(primarySkillId))
        .filter(skill -> skill.emotion_tags() != null && skill.emotion_tags().stream().anyMatch(emotionNames::contains))
        .limit(count - 1) // limit은 스트림의 최대 크기를 제한
        .collect(Collectors.toList());

    List<BehavioralSkill> result = new ArrayList<>();
    primary.ifPresent(result::add);
    result.addAll(others);

    // 결과 리스트의 크기가 count보다 작으면 추가
    if (result.size() < count) {
      behavioralSkills.stream()
          .filter(skill -> !result.contains(skill)) // 이미 포함된 것 제외
          .limit(count - result.size())
          .forEach(result::add);
    }

    return result.stream().limit(count).collect(Collectors.toList()); // 최종 개수 보장
  }
}
