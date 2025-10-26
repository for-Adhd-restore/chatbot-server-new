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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    log.info("handleUserResponse - TO BE IMPLEMENTED");

    // 임시 반환
    return null;
  }


  // 특정 세션의 모든 대화 기록을 불러온다.
  private List<ChatMessageDto> getChatHistory(String id) {
    // TODO: chatMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId) 호출
    log.info("getChatHistory - TO BE IMPLEMENTED");
    return new ArrayList<>(); // 임시로 빈 리스트 반환
  }

  /**
   * 봇의 응답을 MongoDB에 기록
   */
  private void recordBotMessage(String sessionId, String step, String content) {
    // TODO: ChatMessage.builder()...build() 및 chatMessageRepository.save() 호출
    log.info("recordBotMessage - TO BE IMPLEMENTED");
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
        default: // 해당 부분 이해 안됨
          return ChatBotMessage.builder()
              .content("...")
              .type(MessageType.TEXT)
              .build();

    }
  }
}
