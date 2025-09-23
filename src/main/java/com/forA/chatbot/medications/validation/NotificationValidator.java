package com.forA.chatbot.medications.validation;

import com.forA.chatbot.medications.dto.NotificationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

public class NotificationValidator
    implements ConstraintValidator<ValidNotification, NotificationDto> {

  @Override
  public void initialize(ValidNotification constraintAnnotation) {
    // 초기화 로직 (필요시 구현)
  }

  @Override
  public boolean isValid(NotificationDto notification, ConstraintValidatorContext context) {
    if (notification == null) {
      return false;
    }

    // isOn이 null인 경우는 @NotNull에서 처리하므로 여기서는 pass
    if (notification.getIsOn() == null) {
      return true;
    }

    // isOn이 true인데 time이 null이거나 빈 문자열인 경우 검증 실패
    if (Boolean.TRUE.equals(notification.getIsOn())) {
      if (notification.getTime() == null || notification.getTime().trim().isEmpty()) {
        // ValidationException을 던져서 기존 ExceptionAdvice에서 처리되도록 함
        throw new ValidationException("MEDICATION_NOTIFICATION_TIME_REQUIRED");
      }

      // 시간 형식 검증 (HH:mm)
      if (!isValidTimeFormat(notification.getTime())) {
        throw new ValidationException("MEDICATION_INVALID_TIME_FORMAT");
      }
    }

    return true;
  }

  private boolean isValidTimeFormat(String time) {
    if (time == null) {
      return false;
    }

    try {
      String[] parts = time.split(":");
      if (parts.length != 2) {
        return false;
      }

      int hour = Integer.parseInt(parts[0]);
      int minute = Integer.parseInt(parts[1]);

      return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
