package com.forA.chatbot.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.ChatHandler;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.chat.converter.ChatConverter;
import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatMessage.SenderType;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.chat.domain.enums.EmotionType.EmotionState;
import com.forA.chatbot.chat.dto.ChatRequest;
import com.forA.chatbot.chat.dto.ChatResponse;
import com.forA.chatbot.chat.dto.ChatResponse.ButtonOption;
import com.forA.chatbot.chat.dto.ChatResponse.ChatBotMessage;
import com.forA.chatbot.chat.dto.ChatResponse.ChatMessageDto;
import com.forA.chatbot.chat.dto.ChatResponse.MessageType;
import com.forA.chatbot.chat.repository.ChatMessageRepository;
import com.forA.chatbot.chat.repository.ChatSessionRepository;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.notification.scheduler.ChatNotificationScheduler;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

record BehavioralSkill(String chunk_id, String skill_type, List<String> situation_tags,
                       String skill_origin, String skill_name, String description, List<String> step_by_step,
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
  private final ChatNotificationScheduler chatNotificationScheduler;
  private List<BehavioralSkill> behavioralSkills = Collections.emptyList();
  private final ChatConverter chatConverter;

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
  @Transactional
  public ChatResponse initializeSession(Long userId) {
    log.info("채팅 세션 초기화/재개: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserHandler(ErrorStatus.USER_NOT_FOUND));

    ChatSession session;
    ChatBotMessage botMessage;
    List<ChatMessageDto> history = new ArrayList<>();

    // 1. 세션 확인
    Optional<ChatSession> unfinishedSessionOpt = chatSessionRepository
        .findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(userId);

    if (unfinishedSessionOpt.isPresent()) { // 세션 미완
      session = unfinishedSessionOpt.get();
      history = getChatHistory(session.getId());
      botMessage = chatConverter.convertLatestHistoryToBotMessage(history);
      log.info("미완료 세션 재개: {}", session.getId());
    } else { // 세션 완
      Optional<ChatSession> lastSessionOpt = chatSessionRepository.findFirstByUserIdOrderByStartedAtDesc(userId); // 가장 최신 세션 가져오기
      boolean isUserOnboarded = lastSessionOpt
          .map(ChatSession::getOnboardingCompleted)
          .orElse(false);
      String initialStep = isUserOnboarded ? ChatStep.EMOTION_SELECT.name() : ChatStep.GENDER.name();
      session = ChatSession.builder()
          .userId(userId)
          .currentStep(initialStep)
          .onboardingCompleted(isUserOnboarded)
          .startedAt(LocalDateTime.now())
          .build();

      chatSessionRepository.save(session);
      log.info("새 세션 시작: {}, Initial Step: {}", isUserOnboarded, initialStep);

      Set<EmotionType> currentEmotions = parseEmotionsFromString(session.getTemporaryData("selectedEmotions"));
      botMessage =  responseGenerator.getBotMessageForStep(session.getCurrentStep(), user, session.getOnboardingCompleted());
      recordBotMessage(session.getId(), session.getCurrentStep(), botMessage);
      history.add(ChatMessageDto.builder()
          .sender("BOT")
          .content(botMessage.getContent())
          .type(botMessage.getType())
          .options(botMessage.getOptions())
          .sentAt(LocalDateTime.now())
          .build());
    }
    // 8. 최종 응답 반환
    return ChatResponse.builder()
        .sessionId(session.getId())
        .currentStep(session.getCurrentStep())
        .messages(history) // [중간 이탈자]는 이전 기록 전체, [신규/기존]은 봇의 첫 메시지
        .botMessage(botMessage)
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
    ChatBotMessage botMessage = null;
    String selectedEmotionsString = session.getTemporaryData("selectedEmotions");
    Set<EmotionType> selectedEmotions = parseEmotionsFromString(selectedEmotionsString);
    String userSituation = session.getTemporaryData("userSituation");
    String nickname = user.getNickname() != null ? user.getNickname() : "USER";
    try {
      switch (currentStep) {
        case GENDER:
          user.updateGender(Gender.valueOf(userResponse));
          nextStep = ChatStep.BIRTH_YEAR;
          botMessage = responseGenerator.getBotMessageForStep(nextStep.name(), user, false);
          break;
        case BIRTH_YEAR:
          int birthYear = Integer.parseInt(userResponse);
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
          user.updateDisorders(disorders);

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
          session.setTemporaryData("userSituation", userSituation);
          String empathySentence = chatAiService.generateEmpathyResponse(userSituation, selectedEmotions, user);
          String goalPhrase = chatAiService.generateProposalGoalPhrase(userSituation, selectedEmotions);
          nextStep = ChatStep.ACTION_OFFER;
          botMessage = responseGenerator.createActionOfferMessage(nickname, empathySentence, goalPhrase);
          break;
        case ACTION_OFFER:
          if ("YES_PROPOSE".equals(userResponse)) {
            nextStep = ChatStep.ACTION_PROPOSE;
            String skillJson = convertSkillsToJson();
            // 행동 추천 생성
            List<String> recommendedIds = chatAiService.recommendSkillChunkId(userSituation, selectedEmotionsString, skillJson);
            List<BehavioralSkill> recommendedSkills = recommendedIds.stream()
                .map(id -> behavioralSkills.stream()
                    .filter(skill -> skill.chunk_id().equals(id))
                    .findFirst()
                    .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            botMessage = responseGenerator.createActionProposeMessage(recommendedSkills);
          } else if ("NO_PROPOSE".equals(userResponse)) {
            nextStep = ChatStep.CHAT_END;
            String gptComfortMessage = chatAiService.generateSelfSoothingMessages(userSituation, selectedEmotions);
            botMessage = responseGenerator.createAloneComfortMessage(user.getNickname(), gptComfortMessage);
          } else {
            throw new ChatHandler(ErrorStatus.INVALID_BUTTON_SELECTION);
          }
          break;
        case ACTION_PROPOSE:
          String selectedSkillId = userResponse;
          BehavioralSkill selectedSkill = behavioralSkills.stream()
              .filter(skill -> skill.chunk_id().equals(selectedSkillId))
              .findFirst()
              .orElse(null);
          if (selectedSkill == null) {
            log.error("선택하신 스킬을 찾을 수 없습니다: {}", selectedSkillId);
            botMessage = ChatBotMessage.builder()
                .content("선택하신 스킬을 찾을 수 없어요. 다시 선택해주세요.")
                .type(MessageType.TEXT)
                .build();
          } else {
            session.setTemporaryData("selectedSkillId", selectedSkillId);
            session.setTemporaryData("selectedSkillName", selectedSkill.skill_name());
            nextStep = ChatStep.SKILL_SELECT;
            List<String> detailedSteps = chatAiService.generateDetailedSkillSteps(selectedSkill);
            String customDescription = chatAiService.generateSkillDescription(userSituation, selectedEmotions, selectedSkill, user);
            botMessage = responseGenerator.createSkillSelectMessage(customDescription, detailedSteps);
          }
          break;
        case SKILL_SELECT:
          chatNotificationScheduler.scheduleNotification(session.getId(), user.getId());
          nextStep = ChatStep.SKILL_CONFIRM;
          String skillName = session.getTemporaryData("selectedSkillName");
          botMessage = responseGenerator.createSkillConfirmMessage(skillName, nickname);
          break;
        case SKILL_CONFIRM:
          chatNotificationScheduler.cancelNotification(session.getId());
          if ("ACTION_DONE".equals(userResponse)) {
            nextStep = ChatStep.ACTION_FEEDBACK;
            botMessage = responseGenerator.createFeedbackRequestMessage();
          } else if ("ACTION_SKIPPED".equals(userResponse)) {
            nextStep = ChatStep.CHAT_END;

            String skippedSkillId = session.getTemporaryData("selectedSkillId");
            BehavioralSkill skippedSkill = behavioralSkills.stream()
                .filter(s -> s.chunk_id().equals(skippedSkillId))
                .findFirst()
                .orElse(null);

            String gptComfortMessage;
            if (skippedSkill != null) {
              gptComfortMessage = chatAiService.generateActionSkipped(userSituation, selectedEmotions, skippedSkill);
            } else {
              // 스킬을 못찾는 비상시, 기존 '혼자 진정' 로직 사용
              log.warn("SKILL_CONFIRM(SKIP): 스킵한 스킬을 찾을 수 없음: {}", skippedSkillId);
              gptComfortMessage = chatAiService.generateSelfSoothingMessages(userSituation, selectedEmotions);
            }
            botMessage = responseGenerator.createAloneComfortMessage(nickname, gptComfortMessage);
          } else {
            throw new ChatHandler(ErrorStatus.INVALID_BUTTON_SELECTION);
          }
          break;
        case ACTION_FEEDBACK:
          String feedbackValue = userResponse;
          nextStep = ChatStep.CHAT_END;
          botMessage = responseGenerator.createFeedbackDisplayAndClosingMessage(feedbackValue, nickname);
          break;
        case CHAT_END:
          log.info("Chat session {}이 이미 종료되었습니다.", sessionId);
          botMessage = responseGenerator.getBotMessageForStep(currentStep.name(), user, true);
          break;
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
      session.clearTemporaryData();
    }
    chatSessionRepository.save(session);

    // 5. 봇의 다음 응답 메시지 DB에 기록
    recordBotMessage(sessionId, nextStep.name(), botMessage);

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

  private void recordUserMessage(String sessionId, String step, String content) {
    ChatMessage message = ChatMessage.builder()
        .sessionId(sessionId)
        .senderType(SenderType.USER)
        .chatStep(step)
        .messageContent(content)
        .responseCode(content) // 선택/입력값 원본 저장
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);
  }

  private List<ChatMessageDto> getChatHistory(String sessionId) {
    List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
    return messages.stream()
        .map(msg -> {
          MessageType type = null;
          List<ButtonOption> options = null;

          if (msg.getMessageType() != null) {
            try {
              type = MessageType.valueOf(msg.getMessageType()); // String -> Enum
            } catch (IllegalArgumentException e) {
              log.warn("Invalid messageType found in DB: {}", msg.getMessageType());
            }
          }

          if (msg.getOptionsJson() != null && !msg.getOptionsJson().isEmpty()) {
            try {
              options = objectMapper.readValue(msg.getOptionsJson(), new TypeReference<List<ButtonOption>>() {}); // JSON -> List
            } catch (JsonProcessingException e) {
              log.error("Failed to deserialize options JSON from DB: {}", msg.getOptionsJson(), e);
            }
          }
          return ChatMessageDto.builder()
              .sender(msg.getSenderType().name())
              .content(msg.getMessageContent())
              .sentAt(msg.getSentAt())
              .type(type)
              .options(options)
              .build();
        })
        .collect(Collectors.toList());
  }

  private void recordBotMessage(String sessionId, String step, ChatBotMessage botMessage) {
    String optionJson = null;
    if (botMessage.getOptions() != null && !botMessage.getOptions().isEmpty()) {
      try {
        optionJson = objectMapper.writeValueAsString(botMessage.getOptions());
      } catch (JsonProcessingException e) {
        log.error("Json 직렬화에 실패했습니다. ",sessionId, e);
      }
    }
    ChatMessage message = ChatMessage.builder()
        .sessionId(sessionId)
        .senderType(SenderType.BOT)
        .chatStep(step)
        .messageContent(botMessage.getContent())
        .messageType(botMessage.getType().toString())
        .optionsJson(optionJson)
        .sentAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(message);
  }

  private <T extends Enum<T>> Set<T> parseAndValidateMultiSelect(
      String responseValue,
      Function<String, T> valueOf,
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
   * 기존에 미완료된 세션이 있다면 강제로 종료시키고, initializeSession을 호출하여 새 세션을 시작
   */
  @Transactional
  public ChatResponse forceInitializeSession(Long userId) {
    log.info("채팅 세션 강제 새로 시작: {}", userId);

    // 1. 기존 미완료 세션이 있다면 종료시킴
    Optional<ChatSession> unfinishedSessionOpt = chatSessionRepository
        .findFirstByUserIdAndEndedAtIsNullOrderByStartedAtDesc(userId);

    if (unfinishedSessionOpt.isPresent()) {
      ChatSession unfinishedSession = unfinishedSessionOpt.get();
      unfinishedSession.setEndedAt(LocalDateTime.now());
      unfinishedSession.clearTemporaryData();
      chatSessionRepository.save(unfinishedSession);
      log.info("기존 미완료 세션 강제 종료: {}", unfinishedSession.getId());
    }

    // 2. (이제 미완료 세션이 없으므로) initializeSession을 호출하면 'else' 분기를 타게 됨
    return initializeSession(userId);
  }
}
