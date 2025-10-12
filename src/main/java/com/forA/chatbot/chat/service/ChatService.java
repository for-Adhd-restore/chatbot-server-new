package com.forA.chatbot.chat.service;
/*

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.ChatHandler;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.auth.repository.UserRepository;
import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.chat.dto.ChatRequest;
import com.forA.chatbot.chat.dto.ChatResponse;
import com.forA.chatbot.chat.dto.ChatResponse.ButtonOption;
import com.forA.chatbot.chat.dto.ChatResponse.ChatBotMessage;
import com.forA.chatbot.chat.dto.ChatResponse.MessageType;
import com.forA.chatbot.chat.repository.ChatMessageRepository;
import com.forA.chatbot.chat.repository.ChatSessionRepository;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.user.domain.User;
import com.nimbusds.jose.util.Resource;
import java.time.LocalDateTime;
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

  private final ChatSessionRepository chatSessionRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;

  */
/** chatbot 세션 시작 또는 재개 *//*

                          @Transactional
                          public ChatResponse initializeSession(Long userId) {
                            log.info("Chat session initialization for userId: {}", userId);
                            // 1. 기존 또는 재개할 세션 찾기 & 중간이탈 로직 처리
                            Optional<ChatSession> activeSessionOpt = chatSessionRepository
                                .findFirstByUserIdAndOnboardingCompletedFalseOrderByStartedAtDesc(userId);
                            User user = userRepository.findById(userId)
                                .orElseThrow(() -> new UserHandler(ErrorStatus.USER_NOT_FOUND));

                            ChatSession session;
                            if (activeSessionOpt.isPresent()) {
                              session = activeSessionOpt.get();
                              log.info("Resuming existing session: {}", session.getId());
                            } else {
                              // 5.1 완료 상태이거나 신규 유저

                              // 5.1 완료 여부 판단
                              Optional<ChatSession> lastSessionOpt = chatSessionRepository.findFirstByUserIdOrderByStartedAtDesc(userId);
                              boolean isUserOnboarded = lastSessionOpt.isPresent() && lastSessionOpt.get().getOnboardingCompleted();
                              // 완료이면 Emotion부터 아닐 경우 Gender 부터 시작
                              String initialStep = isUserOnboarded ? ChatStep.EMOTION_SELECT.name() : ChatStep.GENDER.name();
                              session = ChatSession.builder()
                                  .userId(userId)
                                  .currentStep(initialStep)
                                  .onboardingCompleted(isUserOnboarded)
                                  .startedAt(LocalDateTime.now())
                                  .build();
                              session = chatSessionRepository.save(session);
                              log.info("Starting new session. Onboarded: {}, Initial Step: {}", isUserOnboarded, initialStep);
                              // 첫 메시지 기록
                              String initialBotMessage = isUserOnboarded
                                  ? getBotMessageForStep(ChatStep.EMOTION_SELECT.name(), session.getId(), user).getContent()
                                  : getBotMessageForStep(ChatStep.GENDER.name(), session.getId(), user).getContent();
                              recordBotMessage(session.getId(), initialStep, initialBotMessage);
                            }

                            // 2. 현재 단계의 응답 생성 (재개 시)
                            ChatResponse.ChatBotMessage botMessage = getBotMessageForStep(session.getCurrentStep(), session.getId(), user);

                            // 3. 기존 메시지 기록 불러오기
                            List<ChatResponse.ChatMessageDto> existingMessages = getChatHistory(session.getCurrentStep(), session.getId());

                            // 4. 세션 업데이트 및 응답 반환
                            session.setLastInteractionAt(LocalDateTime.now());
                            chatSessionRepository.save(session);

                            return ChatResponse.builder()
                                .sessionId(session.getId())
                                .currentStep(session.getCurrentStep())
                                .messages(existingMessages)
                                .botMessage(botMessage)
                                .isCompleted(false)
                                .onboardingCompleted(session.getOnboardingCompleted())
                                .build();
                          }

                          */
/** 유저 응답 처리 및 다음 단계 진행 *//*

                            @Transactional
                            public ChatResponse handleUserResponse(Long userId, String sessionId, ChatRequest request){
                              chatSessionRepository.findById(sessionId).orElseThrow(() -> new ChatHandler(ErrorStatus.SESSION_NOT_FOUND));
                            }
                            private void getChatHistory(String currentStep, String id) {

                            }

                            private void recordBotMessage(String id, String initialStep, String initialBotMessage) {
                            }

                            private ChatBotMessage getBotMessageForStep(String step, String sessionId, User user) {
                              ChatStep chatStep = ChatStep.valueOf(step);
                              switch (chatStep) {
                                case GENDER:
                                  return ChatBotMessage.builder()
                                      .content("안녕하세요," + user.getNickname()
                                          + "님 :) 저는 티모님의 감정과 행동을 함께 살펴주는 AI 상담 친구 '모리' 예요. 모리가 24시 필요할 때 함께 도와줄게요 ADHD, 우울감, 불안 같은 감정들도 비난 없이, 천천히, 함께 마주볼 수 있어요. 편안한 마음으로 이야기를 시작해 주세요")
                                      .type(MessageType.OPTION)
                                      .options(Arrays.asList(
                                          ButtonOption.builder().label("여성").value(Gender.FEMALE.name()).build(),
                                          ButtonOption.builder().label("남성").value(Gender.MALE.name()).build(),
                                          ButtonOption.builder().label("기타").value(Gender.OTHER.name()).build()
                                      ))
                                      .build();
                                case EMOTION_SELECT:
                                  return ChatBotMessage.builder()
                                      .content("감사합니다! 모든 데이터는 마이페이지에서 수정과 삭제가 가능합니다." + user.getNickname()
                                          + "님 지금 어떤 기분이에요? 모리가 알아챌 수 있게 이모지 골라주세요 두 개까지 선택 가능")
                                      .type(MessageType.OPTION)
                                      .options(Arrays.stream(EmotionType.values())
                                          .map(e -> ButtonOption.builder().label(e.getName()).value(e.name())
                                              .isMultiSelect(true).build())
                                          .collect(Collectors.toList()))
                                      .build();
                              }
                            }
                          }
                          */
