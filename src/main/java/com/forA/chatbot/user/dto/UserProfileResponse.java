package com.forA.chatbot.user.dto;

import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class UserProfileResponse {

  private final Long userId;
  private final String nickname;
  private final String gender;
  private final Integer birthYear;
  private final List<String> job;
  private final List<String> mentalConditions;
  private final Map<String, List<String>> symptoms;

  public UserProfileResponse(User user) {
    this.userId = user.getId();
    this.nickname = user.getNickname();
    this.gender = user.getGender() != null ? user.getGender().name().toLowerCase() : null;
    this.birthYear = user.getBirthYear();
    this.job = user.getJobs().stream().map(JobType::getName).collect(Collectors.toList());
    this.mentalConditions =
        user.getDisorders().stream().map(DisorderType::getName).collect(Collectors.toList());
    this.symptoms =
        user.getSymptoms().stream()
            .collect(
                Collectors.groupingBy(
                    symptom -> symptom.getDisorderType().getName(),
                    Collectors.mapping(SymptomType::getDescription, Collectors.toList())));
  }
}
