package com.forA.chatbot.chat.service;

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
import com.forA.chatbot.chat.dto.ChatResponse.ButtonOption;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatSessionRepository chatSessionRepository; // 세션 관리
  private final ChatMessageRepository chatMessageRepository; // 메시지 기록
  private final UserRepository userRepository; // 사용자 정보 조회

  // 3번을 넘긴 후 대화 진행 x
  @Transactional
  public ChatResponse initializeSession(Long userId) {

    log.info("Chat session initialization for userId: {}", userId);
    // 0. 사용자 정보 조회 (닉네임 등을 사용하기 위해)
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserHandler(ErrorStatus.USER_NOT_FOUND));

    ChatSession session;
    List<ChatMessageDto> history;
    boolean isResuming =  false; // 재시작

    // 1. 온보딩 (1 ~ 5)을 완료하지 않은 세션이 있는지 확인 (중간 이탈자)
    Optional<ChatSession> activeSessionOpt = chatSessionRepository
        .findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(userId);

    if (activeSessionOpt.isPresent()) {
      // --- [CASE A: 중간 이탈자] ---
      // 온보딩(1~5) 중에 나갔다가 다시 들어온 경우
      session = activeSessionOpt.get();
      history = getChatHistory(session.getId());// 기존 대화 기록 로드
      isResuming = true;
      log.info("Resuming existing incomplete session: {}", session.getId());
    } else {
      // --- [CASE B: 신규 유저 또는 기존 유저] ---
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
    ChatBotMessage botMessage = getBotMessageForStep(session.getCurrentStep(), user, session.getOnboardingCompleted());

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
   * POST /api/v1/chat/session/{sessionId}
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

    ChatStep nextStep = currentStep; // 다음 단계 (기본값은 현재 단계)
    ChatBotMessage botMessage; // 봇이 보낼 다음 메시지

    // 임시 저장용 변수 (다음 단계에서 사용)
    Set<EmotionType> selectedEmotions = null;
    String userSituation = null;
    // 3. 현재 단계(currentStep)에 따라 로직 분기 (switch)
    try {
      switch (currentStep) {
        case GENDER:
          user.updateGender(Gender.valueOf(userResponse));
          nextStep = ChatStep.BIRTH_YEAR;
          botMessage = getBotMessageForStep(nextStep.name(), user, false);
          break;
        case BIRTH_YEAR:
          int birthYear = Integer.parseInt(userResponse);
          // TODO : 임시 생년 유효범위 세팅
          if(birthYear < 1900 || birthYear > 2030) {
            throw new UserHandler(ErrorStatus.INVALID_YEAR_OF_BIRTH);
          }
          user.updateBirthYear(birthYear);
          nextStep = ChatStep.JOB_TYPE;
          botMessage = getBotMessageForStep(nextStep.name(), user, false);
          break;
        case JOB_TYPE: // 3. 직업 응답 처리
          Set<JobType> jobs = parseAndValidateMultiSelect(userResponse, JobType::valueOf, 2, "직업");
          user.updateJobs(jobs);
          nextStep = ChatStep.DISORDER_TYPE;
          botMessage = getBotMessageForStep(nextStep.name(), user, false);
          break;
        case DISORDER_TYPE:
          Set<DisorderType> disorders = parseAndValidateMultiSelect(userResponse, DisorderType::valueOf, 2, "질환");
          user.updateDisorders(disorders); // User 엔티티에 질환 저장

          if (disorders.stream().anyMatch(d -> d == DisorderType.NONE)) { // '없음' 선택 시
            nextStep = ChatStep.EMOTION_SELECT; // 증상 건너 뛰고 감정 선택으로
            session.setOnboardingCompleted(true); // 온보딩 완료
            botMessage = getBotMessageForStep(nextStep.name(), user, false); // 신규 유저용 6번 멘트
          } else {
            nextStep = ChatStep.SYMPTOM_TYPE; // 다음 단계: 5번(증상)
            // 5단계 질문(증상 버튼)은 동적으로 생성해야 함
            botMessage = createSymptomMessage(disorders);
          }
          break;
        case SYMPTOM_TYPE: // 5. 증상 응답 처리 (온보딩 마지막)
          Set<SymptomType> symptoms = parseAndValidateMultiSelect(userResponse, SymptomType::valueOf, Integer.MAX_VALUE, "증상");
          user.updateSymptoms(symptoms);

          nextStep = ChatStep.EMOTION_SELECT; // 다음 단계: 6번(감정)
          session.setOnboardingCompleted(true); // ★ 온보딩 완료
          botMessage = getBotMessageForStep(nextStep.name(), user, false); // 신규 유저용 6번 멘트
          break;
        case EMOTION_SELECT:
          Set<EmotionType> emotions = parseAndValidateMultiSelect(userResponse, EmotionType::valueOf, 2, "감정");
          // 선택된 감정을 세션에 임시 저장 (SITUATION_INPUT 메시지에 사용)
          session.setTemporaryData("selectedEmotions", selectedEmotions.stream().map(Enum::name).collect(Collectors.joining(",")));

          // 감정 상태에 따른 분기 처리
          if (isPositiveOrSoSo(emotions)) {
            // 긍정/괜찮음 -> 단순 종료
            nextStep = ChatStep.CHAT_END;
            botMessage = createPositiveResponseMessage(emotions);
          } else {
            // 부정/중립 -> 상황 질문
            nextStep = ChatStep.SITUATION_INPUT;
            botMessage = getBotMessageForStep(nextStep.name(), user, true, selectedEmotions);
          }
          break;
        case SITUATION_INPUT:
          userSituation = userResponse;

          // 입력된 상황을 세션에 임시 저장 (GPT에 추후 전달)
          session.setTemporaryData("userSituation", userSituation);
          nextStep = ChatStep.ACTION_PROPOSE; // 다음 단계: 도움 제안
          botMessage = createActionProposeMessage(user.getNickname()); // "추천해도 될까요?" 메시지 생성
          break;
        case CHAT_END:
          // 이미 대화가 종료된 상태
          log.info("Chat session {} already ended.", sessionId);
          botMessage = ChatBotMessage.builder()
              .content("대화가 종료되었습니다. 새 대화를 시작하려면 다시 접속해주세요.")
              .type(MessageType.TEXT)
              .build();
          break;
        // TODO : 6.1.1 단계 이후는 나중에 구현
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

  private ChatBotMessage createActionProposeMessage(String nickname) {
    return ChatBotMessage.builder()
        .content("그 상황에서 마음이 많이 복잡하고 힘들었겠어요. 다시 마주해야 한다고 생각하니 불안한 감정이 드는 게 정말 자연스러운 일이에요. 모리가 " + nickname + "님의 마음을 진정시키는데 도움이 될 수 있는 방법을 추천해도 될까요?")
        .type(MessageType.OPTION)
        .options(Arrays.asList(
            ButtonOption.builder().label("응, 뭔데?").value("YES_PROPOSE").build(),
            ButtonOption.builder().label("아니 혼자 진정하고 싶어").value("NO_PROPOSE").build()
        ))
        .build();
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
   * 긍정/괜찮음 응답(고정 멘트)을 생성
   */
  private ChatBotMessage createPositiveResponseMessage(Set<EmotionType> emotions) {
    String content;
    if (emotions.size() == 1) {
      EmotionType emotion = emotions.iterator().next();
      switch (emotion) {
        case EXCITED: content = "무언가 기대되는 일이 있었나 봐요! 그 에너지, 좋아요 😆"; break;
        case JOY: content = "즐거운 순간이 있었군요. 그 기분 오래오래 간직해요 😊"; break;
        case PROUD: content = "오늘 스스로에게 칭찬해줄 일이 있었나 봐요! 정말 잘했어요 👏"; break;
        case HAPPY: content = "행복하다고 느껴지는 순간, 너무 소중하죠. 지금 이 마음을 기억해요 💛"; break;
        case FLUTTER: content = "마음이 간질간질, 좋은 일이 기다리고 있나 봐요! 설렘은 삶의 활력소예요 🌸"; break;
        case SO_SO: content = "큰 감정 변화는 없지만, 이런 날도 충분히 괜찮아요. 그냥 있는 그대로의 하루도 소중해요 🍃"; break;
        default: content = "긍정적인 감정을 느끼셨군요! 좋아요.";
      }
    } else if (emotions.size() == 2) {
      Map<Set<EmotionType>, String> combinationMessages = Map.ofEntries(
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.JOY), "신나고 즐거운 하루였네요! 이런 기분이 오래오래 이어졌으면 좋겠네요. 😄🎉"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.PROUD), "신나고 뿌듯한 하루를 보내셨네요. 오늘의 성취가 모리도 뿌듯하게 느껴지네요. 😆👏"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.HAPPY), "신나고 행복한 하루였네요. 좋은 일이 가득해서 저도 기분이 좋아지네요. 😊💛"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.FLUTTER), "신나고 설레는 하루였네요. 앞으로도 기대되는 일이 많으시길 바랄게요. 💫🌸"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.SO_SO), "신나는 순간도 있었고, 평범한 시간도 있었네요. 여러 감정이 어우러진 하루였던 것 같네요.🎭"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.PROUD), "즐겁고 뿌듯한 하루를 보내셨네요. 오늘의 좋은 기억이 오래 남았으면 해요. 😊👏"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.HAPPY), "즐거움과 행복이 함께한 하루였네요. 저도 덩달아 미소가 지어지네요. 😄💛"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.FLUTTER), "즐겁고 설레는 하루였네요. 새로운 시작이나 만남이 있었던 걸까요? 앞으로도 좋은 일이 가득하길 바랄게요. 🌟😊"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.SO_SO), "즐거운 순간도 있었고, 평범한 시간도 있었네요. 그런 하루도 충분히 의미 있네요. 🍃🙂"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.HAPPY), "뿌듯함과 행복이 함께한 하루였네요. 오늘의 성취가 큰 기쁨이 되었겠어요. 👏😊"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.FLUTTER), "뿌듯하고 설레는 하루였네요. 앞으로도 좋은 변화가 이어지길 바랄게요. 🌱💫"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.SO_SO), "뿌듯한 순간과 평범한 시간이 함께한 하루였네요. 그런 균형이 참 소중하네요. ⚖️🍀"),
          Map.entry(Set.of(EmotionType.HAPPY, EmotionType.FLUTTER), "행복하고 설레는 하루였네요. 좋은 일이 곧 찾아올 것 같은 느낌이네요. 💛🌸"),
          Map.entry(Set.of(EmotionType.HAPPY, EmotionType.SO_SO), "행복한 순간도 있었고, 평범한 시간도 있었네요. 오늘 하루도 잘 보내셨네요. 🌤️🙂"),
          Map.entry(Set.of(EmotionType.FLUTTER, EmotionType.SO_SO), "설레는 순간도 있었고, 평범한 시간도 있었네요. 다양한 감정이 어우러진 하루였던 것 같네요. 🎈🍃")
      );
      content = combinationMessages.getOrDefault(emotions, "긍정적인 감정들이 함께했네요. 멋진 하루예요! 🌟");
    } else {
      content = "오늘 기분이 좋으셨군요!";
    }
    return ChatBotMessage.builder().content(content).type(MessageType.TEXT).build();
  }

  /**
   * 4단계(질환) 응답을 기반으로 5단계(증상) 질문지를 동적으로 생성
   */
  private ChatBotMessage createSymptomMessage(Set<DisorderType> disorders) {
    // 4단계에서 선택한 질환(disorders)에 해당하는 증상들만 가져오기
    Set<SymptomType> symptoms = SymptomType.getByDisorderTypes(disorders);

    List<ButtonOption> options = symptoms.stream()
        .map(s -> ButtonOption.builder()
            .label(s.getDescription())
            .value(s.name())
            .isMultiSelect(true)
            .build())
        .collect(Collectors.toList());

    return ChatBotMessage.builder()
        .content("주로 힘들어 하는 일은 어떤건가요? 모리가 참고해서 도와줄게요")
        .type(MessageType.OPTION)
        .options(options)
        .build();
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

  private ChatBotMessage getBotMessageForStep(String step, User user, boolean isUserOnboarded, Set<EmotionType> selectedEmotions) {
    ChatStep chatStep = ChatStep.valueOf(step);
    String nickname = user.getNickname() != null ? user.getNickname() : "USER";

    switch (chatStep) {
      case GENDER:
        return ChatBotMessage.builder()
            .content("안녕하세요," + nickname
                + "님 :) 저는 티모님의 감정과 행동을 함께 살펴주는 AI 상담 친구 '모리' 예요. 모리가 24시 필요할 때 함께 도와줄게요 ADHD, 우울감, 불안 같은 감정들도 비난 없이, 천천히, 함께 마주볼 수 있어요. 편안한 마음으로 이야기를 시작해 주세요\n"
                + "상담을 시작하기 전, 상담에 도움이 될 수 있는 질문을 몇가지 하겠습니다. 먼저, 성별을 선택해주세요!")
            .type(MessageType.OPTION)
            .options(Arrays.asList(
                ButtonOption.builder().label("여성").value(Gender.FEMALE.name()).build(),
                ButtonOption.builder().label("남성").value(Gender.MALE.name()).build(),
                ButtonOption.builder().label("기타").value(Gender.OTHER.name()).build()
            ))
            .build();
      case BIRTH_YEAR: // 2. 생년 입력
        return ChatBotMessage.builder()
            .content("알맞은 도움을 드리기 위해 연령대가 중요한 기준이 됩니다. 태어난 연도를 4자리 숫자를 알려주세요!")
            .type(MessageType.INPUT)
            .build();
      case JOB_TYPE: // 3. 직업 선택 - 최대 2개까지 선택되도록 구현
        return ChatBotMessage.builder()
            .content("지금 하는 일이 어떻게 되는지 궁금해요! 최대 2개까지 선택할 수 있어요!")
            .type(MessageType.OPTION)
            .options(Arrays.stream(JobType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      case DISORDER_TYPE: // 4. 정신 질환 선택 - 최대 2개까지 선택되도록 구현
        return ChatBotMessage.builder()
            .content("앓고 있는 정신 질환이 있으신가요? 최대 2개까지 선택할 수 있어요!")
            .type(MessageType.OPTION)
            .options(Arrays.stream(DisorderType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      // 5. SYMPTOM_TYPE은 동적이므로 여기서는 처리하지 않음 (createSymptomMessage가 대신 처리)
      case EMOTION_SELECT: // 6. 감정 선택
        String content = isUserOnboarded ?
            String.format("안녕하세요, %s님! 모리예요! 🐾\n오늘은 기분이 어때요? 모리가 눈치 빠르게 알아챌 수 있게 이모지 두 개만 콕! 찍어주세요.", nickname) :
            String.format("감사합니다! 모든 데이터는 마이페이지에서 수정과 삭제가 가능합니다. %s님 지금 어떤 기분이에요? 모리가 알아챌 수 있게 이모지 골라주세요", nickname);

        return ChatBotMessage.builder()
            .content(content)
            .type(MessageType.OPTION)
            .options(Arrays.stream(EmotionType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      case SITUATION_INPUT:
        // 선택된 감정 이름을 가져와서 메시지에 포함
        String emotionNames = selectedEmotions.stream()
            .map(EmotionType::getName)
            .collect(Collectors.joining(" ", "지금 ", "상태이시군요."));
        return ChatBotMessage.builder()
            .content(emotionNames + " 혹시 어떤 일이 있었는지 이야기 해줄 수 있나요?")
            .type(MessageType.INPUT)
            .build();
      case CHAT_END: // 종료
        return ChatBotMessage.builder()
            .content("대화가 종료되었습니다.")
            .type(MessageType.TEXT)
            .build();
      default:
        log.warn("getBotMessageForStep: Unhandled step: {}", step);
        return ChatBotMessage.builder().content("...").type(MessageType.TEXT).build();    }
  }
  // 오버로딩: selectedEmotions가 필요 없는 경우 호출하는 메서드 (이것이 handleUserResponse 등에서 주로 사용됨)
  private ChatBotMessage getBotMessageForStep(String step, User user, boolean isUserOnboarded) {
    return getBotMessageForStep(step, user, isUserOnboarded, Set.of()); // 비어있는 Set 전달
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
}
