package com.forA.chatbot.medications.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotificationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotification {
  String message() default "MEDICATION_NOTIFICATION_TIME_REQUIRED"; // ErrorStatus의 enum 이름
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}