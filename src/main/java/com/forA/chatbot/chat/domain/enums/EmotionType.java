package com.forA.chatbot.chat.domain.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmotionType {
  // 중립 감정
  SO_SO(1, "그저그런(괜찮음)", EmotionState.NEUTRAL),
  LISTLESSNESS(2, "무기력함", EmotionState.NEUTRAL),
  LONGING(3, "그리움", EmotionState.NEUTRAL),
  BOREDOM(4, "지루함", EmotionState.NEUTRAL),

  // 긍정 감정
  EXCITED(5, "신남", EmotionState.POSITIVE),
  JOY(6, "즐거움", EmotionState.POSITIVE),
  PROUD(7, "뿌듯함", EmotionState.POSITIVE),
  HAPPY(8, "행복함", EmotionState.POSITIVE),
  FLUTTER(9, "설렘", EmotionState.POSITIVE),

  // 부정 감정
  ANGER(10, "화남", EmotionState.NEGATIVE),
  ANXIETY(11, "불안함", EmotionState.NEGATIVE),
  REGRET(12, "후회됨", EmotionState.NEGATIVE),
  TENSION(13, "긴장됨", EmotionState.NEGATIVE),
  SADNESS(14, "슬픔", EmotionState.NEGATIVE),
  ANNOYED(15, "귀찮음", EmotionState.NEGATIVE),
  EXHAUSTION(16, "지침", EmotionState.NEGATIVE),
  TIRED(17, "피곤함", EmotionState.NEGATIVE);

  private final int id;
  private final String name;
  private final EmotionState state;

  /** 감정 상태 분류: 긍정, 중립, 부정 */
  @Getter
  @RequiredArgsConstructor
  public enum EmotionState {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
  }

  public static EmotionType fromId(int id) {
    return Arrays.stream(values())
        .filter(emotion -> emotion.id == id)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid EmotionType id: " + id));
  }
}