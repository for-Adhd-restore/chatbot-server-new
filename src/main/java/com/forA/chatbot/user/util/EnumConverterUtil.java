package com.forA.chatbot.user.util;

import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumConverterUtil {

    public static Set<JobType> convertJobsToEnum(List<String> jobNames) {
        if (jobNames == null) {
            return new HashSet<>();
        }
        
        return jobNames.stream()
                .map(name -> Arrays.stream(JobType.values())
                        .filter(job -> job.getName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직업입니다: " + name)))
                .collect(Collectors.toSet());
    }

    public static Set<DisorderType> convertDisordersToEnum(List<String> disorderNames) {
        if (disorderNames == null) {
            return new HashSet<>();
        }
        
        return disorderNames.stream()
                .map(name -> Arrays.stream(DisorderType.values())
                        .filter(disorder -> disorder.getName().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 질환입니다: " + name)))
                .collect(Collectors.toSet());
    }

    public static Set<SymptomType> convertSymptomsToEnum(Map<String, List<String>> symptomsMap) {
        if (symptomsMap == null) {
            return new HashSet<>();
        }
        
        Set<SymptomType> symptoms = new HashSet<>();
        
        for (Map.Entry<String, List<String>> entry : symptomsMap.entrySet()) {
            String disorderName = entry.getKey();
            List<String> symptomNames = entry.getValue();
            
            // 해당 질환 찾기
            DisorderType disorderType = Arrays.stream(DisorderType.values())
                    .filter(disorder -> disorder.getName().equals(disorderName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 질환입니다: " + disorderName));

            // 증상들을 enum으로 변환
            for (String symptomName : symptomNames) {
                SymptomType symptom = Arrays.stream(SymptomType.values())
                        .filter(s -> s.getDescription().equals(symptomName) && s.getDisorderType() == disorderType)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "유효하지 않은 증상입니다: " + symptomName + " (질환: " + disorderName + ")"));
                symptoms.add(symptom);
            }
        }
        
        return symptoms;
    }
}