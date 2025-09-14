package com.forA.chatbot.apiPayload.code.status;

import com.forA.chatbot.apiPayload.code.BaseErrorCode;
import com.forA.chatbot.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

  // For test
  TEMP_EXCEPTION(HttpStatus.BAD_REQUEST, "TEMP4001", "이거는 테스트"),

  // 가장 일반적인 응답
  _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
  _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
  _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
  _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

  // AUTH
  APPLE_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "AUTH4001", "Apple 토큰이 유효하지 않습니다."),
  APPLE_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH4002", "Apple 토큰이 만료되었습니다."),
  APPLE_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4003", "Apple 로그인에 실패했습니다."),
  APPLE_PUBLIC_KEY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "Apple 공개키 조회에 실패했습니다."),

  TEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TEST500", "테스트를 실패하였습니다");
  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ErrorReasonDTO getReason() {
    return ErrorReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(false)
        .build();
  }

  @Override
  public ErrorReasonDTO getReasonHttpStatus() {
    return ErrorReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(false)
        .httpStatus(httpStatus)
        .build();
  }
}
