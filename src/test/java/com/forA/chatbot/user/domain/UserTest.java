package com.forA.chatbot.user.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.forA.chatbot.apiPayload.code.status.ErrorStatus;
import com.forA.chatbot.apiPayload.exception.handler.UserHandler;
import com.forA.chatbot.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  @DisplayName("성별 최초 업데이트 성공")
  void updateGender_Success_FirstTime() {
    // given
    User user = User.builder()
        .gender(Gender.UNKNOWN) // 초기 상태
        .build();

    // when
    user.updateGender(Gender.FEMALE);

    // then
    assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
  }

  @Test
  @DisplayName("멱등성 테스트: 이미 설정된 성별과 '동일한' 성별로 요청 시 성공해야 함")
  void updateGender_Success_SameValue() {
    // given
    User user = User.builder()
        .gender(Gender.FEMALE)
        .build();

    // when
    // 다시 FEMALE로 업데이트 시도 (기존에는 여기서 에러 발생했음)
    user.updateGender(Gender.FEMALE);

    // then
    // 에러 없이 통과하고 값은 유지되어야 함
    assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
  }

  @Test
  @DisplayName("실패 테스트: 이미 설정된 성별과 '다른' 성별로 요청 시 예외 발생")
  void updateGender_Fail_DifferentValue() {
    // given
    User user = User.builder()
        .gender(Gender.FEMALE) // FEMALE로 설정됨
        .build();

    // when & then
    // MALE로 변경 시도 -> 예외 발생해야 함
    assertThatThrownBy(() -> user.updateGender(Gender.MALE))
        .isInstanceOf(UserHandler.class)
        .hasFieldOrPropertyWithValue("code", ErrorStatus.USER_GENDER_ALREADY_EXIST);
  }
}
