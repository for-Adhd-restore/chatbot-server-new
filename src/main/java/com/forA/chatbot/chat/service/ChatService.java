package com.forA.chatbot.chat.service;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.ChatHandler;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.chat.domain.ChatMessage;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
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
      Optional<ChatSession> lastSessionOpt = chatSessionRepository.findFirstByUserIdOrderByStartedAtDesc(
          userId);

      // 사용자가 온보딩을 완료한 적이 있는지 여부
      Boolean isUserOnboarded = lastSessionOpt
          .map(ChatSession::getOnboardingCompleted)
          .orElse(false);

      // 3. 시작 단계 결정
      String initialStep;
      if (isUserOnboarded) {
        // [기존 유저] -> 6. 감정 선택부터 시작
        initialStep = ChatStep.EMOTION_SELECT.name();
      } else {
        // [신규 유저] -> 1. 성별 선택부터 시작
        initialStep = ChatStep.GENDER.name();
      }
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

    // 6. (중요) 새 세션인 경우에만 봇의 첫 메시지를 DB에 기록하고, history에도 추가
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
    // TODO: 1~5, 6번 로직의 핵심인 switch-case 구현
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
          Set<JobType> jobs = parseAndValidateJobs(userResponse); // "2개 이하" 유효성 검사
          user.updateJobs(jobs);
          nextStep = ChatStep.DISORDER_TYPE;
          botMessage = getBotMessageForStep(nextStep.name(), user, false);
          break;

        case DISORDER_TYPE:
          Set<DisorderType> disorders = parseAndValidateDisorders(userResponse); // "2개 이하" 유효성 검사
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
          Set<SymptomType> symptoms = parseSymptoms(userResponse);
          user.updateSymptoms(symptoms);

          nextStep = ChatStep.EMOTION_SELECT; // 다음 단계: 6번(감정)
          session.setOnboardingCompleted(true); // ★ 온보딩 완료
          botMessage = getBotMessageForStep(nextStep.name(), user, false); // 신규 유저용 6번 멘트
          break;
        // TODO : 6단계 이후는 나중에 구현
        default:
          log.warn("handleUserResponse: Unhandled step: {}", currentStep);
          throw new IllegalArgumentException("처리할 수 없는 단계입니다.");
      }
    } catch (IllegalArgumentException e) {
      // 유효성 검사 실패
      log.warn("Invalid user response: {} for step: {}. Error: {}", userResponse, currentStep, e.getMessage());

      // 사용자에게 에러 메시지 전송 (현재 단계 유지)
      botMessage = ChatBotMessage.builder()
          .content(e.getMessage() + "\n다시 선택해주세요.") // e.g. "직업은 최대 2개까지 선택 가능합니다."
          .type(MessageType.TEXT)
          .build();
      // nextStep은 기본값(currentStep)을 유지
    }

    // 4. 유저 정보 및 세션 상태 저장
    userRepository.save(user); // 1~5단계에서 변경된 유저 정보(성별, 생년 등)를 DB에 최종 저장
    session.setCurrentStep(nextStep.name());
    session.setLastInteractionAt(LocalDateTime.now());
    chatSessionRepository.save(session);

    // 5. 봇의 다음 응답 메시지 DB에 기록
    recordBotMessage(sessionId, nextStep.name(), botMessage.getContent());

    // 6. 최종 응답 반환
    return ChatResponse.builder() //
        .sessionId(session.getId())
        .currentStep(nextStep.name())
        .botMessage(botMessage)
        .isCompleted(nextStep == ChatStep.CHAT_END) // (아직 CHAT_END 없음)
        .onboardingCompleted(session.getOnboardingCompleted()) //
        .build();
  }

  private Set<SymptomType> parseSymptoms(String responseValue) {
    String[] selectedSymptoms = responseValue.split(",");
    if (selectedSymptoms.length > 2 ||  selectedSymptoms.length < 1) {
      throw new ChatHandler(ErrorStatus.INVALID_SYMPTOMS_COUNT);
    }
    return Arrays.stream(selectedSymptoms)
        .map(SymptomType::valueOf)
        .collect(Collectors.toSet());
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

  private Set<DisorderType> parseAndValidateDisorders(String responseValue) {
    String[] selectedDisorders = responseValue.split(",");
    if (selectedDisorders.length > 2 || selectedDisorders.length < 1) {
      throw new ChatHandler(ErrorStatus.INVALID_DISORDER_COUNT);
    }
    return Arrays.stream(selectedDisorders)
        .map(DisorderType::valueOf) //
        .collect(Collectors.toSet());
  }

  private Set<JobType> parseAndValidateJobs(String responseValue) {
    // 프론트에서 "JOB1,JOB2" 형식으로 보낸다고 가정
    String[] selectedJobs = responseValue.split(",");
    if (selectedJobs.length > 2 || selectedJobs.length < 1) {
      throw new ChatHandler(ErrorStatus.INVALID_JOB_COUNT);
    }
    return Arrays.stream(selectedJobs)
        .map(JobType::valueOf)
        .collect(Collectors.toSet());
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

  private ChatBotMessage getBotMessageForStep(String step, User user, boolean isUserOnboarded) {
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
      case SITUATION_INPUT: // 6.1 상황 입력 (타입: INPUT)
        //TODO : (000 부분은 나중에 동적으로 채워야 함)
        return ChatBotMessage.builder()
            .content("지금 000고 000하시군요. 혹시 어떤 일이 있었는지 이야기 해줄 수 있나요?")
            .type(MessageType.INPUT)
            .build();
      default:
        log.warn("getBotMessageForStep: Unhandled step: {}", step);
        return ChatBotMessage.builder()
            .content("다음 단계로 진행합니다.") // 임시 메시지
            .type(MessageType.TEXT)
            .build();
    }
  }
}
