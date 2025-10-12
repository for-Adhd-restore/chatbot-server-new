package com.forA.chatbot.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatResponse {

  private String sessionId;
  private String currentStep; // 현재 대화 단계
  private List<ChatMessageDto> messages; // 현재까지의 대화 기록 (세션 재개 시 유용)
  private ChatBotMessage botMessage; // 챗봇의 다음 메시지
  private Boolean isCompleted; // 현재 단계가 완료되었는지 여부
  private Boolean onboardingCompleted; // 5.1 데이터 수집 완료 여부

  @Getter
  @Builder
  public static class ChatMessageDto {
    private String sender; // USER, BOT
    private String content;
    private LocalDateTime sentAt;
  }

  @Getter
  @Builder
  public static class ChatBotMessage {
    private String content;
    private MessageType type; // TEXT, OPTION, INPUT
    private List<ButtonOption> options; // OPTION 타입일 때 제공되는 버튼 목록
  }

  public enum MessageType {
    TEXT, // 일반 텍스트 응답 (GPT AI와 대화할 경우)
    OPTION, // 버튼 선택형 응답
    INPUT // 키보드 입력형 응답 (ex. 생년 입력, 현재 상황 입력)
  }

  @Getter
  @Builder
  public static class ButtonOption {
    private String label;
    private String value; // 백엔드로 전달될 실제 값 (예: MALE, 2000, DEPRESSION)
    private boolean isMultiSelect; // 다중 선택 가능 여부
  }
}
