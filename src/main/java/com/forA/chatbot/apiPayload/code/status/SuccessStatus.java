package com.forA.chatbot.apiPayload.code.status;

import com.forA.chatbot.apiPayload.code.BaseCode;
import com.forA.chatbot.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

  // 일반적인 응답
  _OK(HttpStatus.OK, "COMMON200", "성공입니다."),
  _CREATED(HttpStatus.CREATED, "COMMON201", "생성에 성공했습니다."),

  // MEDICATION 관련
  MEDICATION_CREATED(HttpStatus.CREATED, "MEDICATION2001", "약 복용 계획이 성공적으로 생성되었습니다."),
  MEDICATION_LOG_CREATED(HttpStatus.CREATED, "MEDICATION2002", "약 복용 기록이 성공적으로 생성되었습니다."),
  MEDICATION_LIST_RETRIEVED(HttpStatus.OK, "MEDICATION2003", "오늘의 약 복용 계획을 성공적으로 조회하엿습니다."),
  MEDICATION_UPDATED(HttpStatus.OK, "MEDICATION2004", "복용 계획이 성공적으로 수정되었습니다.")
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ReasonDTO getReason() {
    return ReasonDTO.builder().message(message).code(code).isSuccess(true).build();
  }

  @Override
  public ReasonDTO getReasonHttpStatus() {
    return ReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(true)
        .httpStatus(httpStatus)
        .build();
  }
}
