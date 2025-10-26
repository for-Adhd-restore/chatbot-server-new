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
  TOKEN_REFRESH_FAILED(HttpStatus.BAD_REQUEST, "AUTH4004", "토큰 재발급에 실패했습니다."),
  EXPIRED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "AUTH4005", "REFRESH TOKEN이 만료되었습니다"),

  // TEMP LOGIN
  TEMP_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4003", "임시 로그인에 실패했습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4004", "존재하지 않는 사용자입니다."),

  // MEDICATION - 약물 관련 에러 코드들
  MEDICATION_PLAN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDICATION002", "해당 복용 계획에 접근할 권한이 없습니다."),
  MEDICATION_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDICATION4001", "사용자를 찾을 수 없습니다."),
  MEDICATION_INVALID_TIME_FORMAT(
      HttpStatus.BAD_REQUEST, "MEDICATION4002", "시간 형식이 올바르지 않습니다. HH:mm 형식으로 입력해주세요."),
  MEDICATION_NOTIFICATION_TIME_REQUIRED(
      HttpStatus.BAD_REQUEST, "MEDICATION4003", "알림이 활성화된 경우 알림 시간은 필수입니다."),
  MEDICATION_INVALID_TIME_RANGE(
      HttpStatus.BAD_REQUEST, "MEDICATION4004", "시간은 00:00 ~ 23:59 범위 내에서 입력해주세요."),
  MEDICATION_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "MEDICATION4005", "약 이름은 최대 20자까지 입력 가능합니다."),
  MEDICATION_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDICATION4006", "약 복용 계획을 찾을 수 없습니다."),
  MEDICATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDICATION4007", "해당 약 복용 계획에 대한 접근 권한이 없습니다."),
  MEDICATION_INVALID_DAY_OF_WEEK(HttpStatus.BAD_REQUEST, "MEDICATION4008", "올바르지 않은 요일 형식입니다."),
  MEDICATION_TYPE_TAGS_EMPTY(HttpStatus.BAD_REQUEST, "MEDICATION4009", "약 종류 태그는 최소 1개 이상 필요합니다."),
  MEDICATION_TAKE_DAYS_EMPTY(
      HttpStatus.BAD_REQUEST, "MEDICATION4010", "복용할 요일은 최소 1개 이상 선택해야 합니다."),
  MEDICATION_CONDITION_REQUIRED(HttpStatus.BAD_REQUEST, "MEDICATION4011", "컨디션을 입력해주세요."),
  MEDICATION_INVALID_DATE_FORMAT(
      HttpStatus.BAD_REQUEST, "MEDICATION4012", "날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요."),

  // Notification 관련
  _FCM_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION5001", "FCM 메시지 전송에 실패했습니다."),

  TEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TEST500", "테스트를 실패하였습니다"),

  // User 관련 에러
  USER_GENDER_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "USER4001", "유저 성별을 변경할 수 없습니다."),
  USER_BIRTH_YEAR_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "USER4002", "유저 생년을 변경할 수 없습니다."),
  INVALID_YEAR_OF_BIRTH(HttpStatus.BAD_REQUEST, "USER4003", "유저 생년이 유효하지 않습니다."),

  // Chat 관련 에러
  SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "CHAT4001", "존재하는 세션이 아닙니다."),
  INVALID_JOB_COUNT(HttpStatus.BAD_REQUEST, "CHAT4002", "직업 입력 개수가 올바르지 않습니다."),
  INVALID_DISORDER_COUNT(HttpStatus.BAD_REQUEST, "CHAT4003", "질환 입력 개수가 올바르지 않습니다."),
  INVALID_SYMPTOMS_COUNT(HttpStatus.BAD_REQUEST, "CHAT4004", "증상 입력 개수가 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ErrorReasonDTO getReason() {
    return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).build();
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
