package com.forA.chatbot.user.dto;

import com.forA.chatbot.enums.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class UserProfileUpdateRequest {

    @Size(max = 20, message = "닉네임은 20자 이하여야 합니다")
    private String nickname;

    private Gender gender;

    @Min(value = 1900, message = "유효한 생년을 입력해주세요")
    @Max(value = 2030, message = "유효한 생년을 입력해주세요")
    private Integer birthYear;

    @Size(max = 2, message = "직업은 최대 2개까지 선택 가능합니다")
    private List<String> jobs;

    @Size(min = 1, max = 2, message = "정신질환은 1~2개 선택해주세요")
    private List<String> disorders;

    private Map<String, List<String>> symptoms;
}