package com.forA.chatbot.chat.service;

import com.forA.chatbot.chat.domain.ChatSession;
import com.forA.chatbot.chat.domain.enums.ChatStep;
import com.forA.chatbot.chat.domain.enums.EmotionType;
import com.forA.chatbot.chat.dto.ChatResponse.ButtonOption;
import com.forA.chatbot.chat.dto.ChatResponse.ChatBotMessage;
import com.forA.chatbot.chat.dto.ChatResponse.MessageType;
import com.forA.chatbot.enums.Gender;
import com.forA.chatbot.user.domain.User;
import com.forA.chatbot.user.domain.enums.DisorderType;
import com.forA.chatbot.user.domain.enums.JobType;
import com.forA.chatbot.user.domain.enums.SymptomType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatResponseGenerator {

  public ChatBotMessage getBotMessageForStep(String step, User user, boolean isUserOnboarded, Set<EmotionType> selectedEmotions) {
    ChatStep chatStep = ChatStep.valueOf(step);
    String nickname = user.getNickname() != null ? user.getNickname() : "USER";

    switch (chatStep) {
      case GENDER:
        return ChatBotMessage.builder()
            .content("안녕하세요," + nickname
                + "님 :) 저는 티모님의 감정과 행동을 함께 살펴주는 AI 상담 친구 '모리' 예요. 모리가 24시 필요할 때 함께 도와줄게요 ADHD, 우울감, 불안 같은 감정들도 비난 없이, 천천히, 함께 마주볼 수 있어요. 편안한 마음으로 이야기를 시작해 주세요\n"
                + "상담을 시작하기 전, 상담에 도움이 될 수 있는 질문을 몇가지 하겠습니다. 먼저, 성별을 선택해주세요!")
            .type(MessageType.OPTION)
            .options(Arrays.asList(
                ButtonOption.builder().label("여성").value(Gender.FEMALE.name()).build(),
                ButtonOption.builder().label("남성").value(Gender.MALE.name()).build(),
                ButtonOption.builder().label("기타").value(Gender.OTHER.name()).build()
            ))
            .build();
      case BIRTH_YEAR: // 2. 생년 입력
        return ChatBotMessage.builder()
            .content("알맞은 도움을 드리기 위해 연령대가 중요한 기준이 됩니다. 태어난 연도를 4자리 숫자를 알려주세요!")
            .type(MessageType.INPUT)
            .build();
      case JOB_TYPE: // 3. 직업 선택 - 최대 2개까지 선택되도록 구현
        return ChatBotMessage.builder()
            .content("지금 하는 일이 어떻게 되는지 궁금해요! 최대 2개까지 선택할 수 있어요!")
            .type(MessageType.OPTION)
            .options(Arrays.stream(JobType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      case DISORDER_TYPE: // 4. 정신 질환 선택 - 최대 2개까지 선택되도록 구현
        return ChatBotMessage.builder()
            .content("앓고 있는 정신 질환이 있으신가요? 최대 2개까지 선택할 수 있어요!")
            .type(MessageType.OPTION)
            .options(Arrays.stream(DisorderType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      // 5. SYMPTOM_TYPE은 동적이므로 여기서는 처리하지 않음 (createSymptomMessage가 대신 처리)
      case EMOTION_SELECT: // 6. 감정 선택
        String content = isUserOnboarded ?
            String.format("안녕하세요, %s님! 모리예요! 🐾\n오늘은 기분이 어때요? 모리가 눈치 빠르게 알아챌 수 있게 이모지 두 개만 콕! 찍어주세요.", nickname) :
            String.format("감사합니다! 모든 데이터는 마이페이지에서 수정과 삭제가 가능합니다. %s님 지금 어떤 기분이에요? 모리가 알아챌 수 있게 이모지 골라주세요", nickname);

        return ChatBotMessage.builder()
            .content(content)
            .type(MessageType.OPTION)
            .options(Arrays.stream(EmotionType.values())
                .map(e -> ButtonOption.builder().label(e.getName()).value(e.name()).isMultiSelect(true).build())
                .collect(Collectors.toList()))
            .build();
      case SITUATION_INPUT:
        // 선택된 감정 이름을 가져와서 메시지에 포함
        String emotionNames = selectedEmotions.stream()
            .map(EmotionType::getName)
            .collect(Collectors.joining(" ", "지금 ", "상태이시군요."));
        return ChatBotMessage.builder()
            .content(emotionNames + " 혹시 어떤 일이 있었는지 이야기 해줄 수 있나요?")
            .type(MessageType.INPUT)
            .build();
      case SKILL_SELECT:
        return ChatBotMessage.builder()
            .content("좋아요, 그럼 지금 이 감정에 도움이 될 수 있는 방법들을 하나씩 소개해볼게요." + "지금 감정에 도움이 될 수 있는 방법들을 소개했어요. 이 중에서 하나 골라 함께 해볼까요?")
            .type(MessageType.OPTION)
            .options(Arrays.asList( // TODO : 수정 필요 - 현재 임시 버튼
                ButtonOption.builder().label("스킬1").value("SKILL_1").build(),
                ButtonOption.builder().label("스킬2").value("SKILL_2").build()
            ))
            .build();
      case CHAT_END: // 종료
        return ChatBotMessage.builder()
            .content("대화가 종료되었습니다.")
            .type(MessageType.TEXT)
            .build();
      default:
        log.warn("getBotMessageForStep: Unhandled step: {}", step);
        return ChatBotMessage.builder().content("...").type(MessageType.TEXT).build();    }
  }
  public ChatBotMessage getBotMessageForStep(String step, User user, boolean isUserOnboarded) {
    return getBotMessageForStep(step, user, isUserOnboarded, Set.of());
  }
  /**
   * 4단계(질환) 응답을 기반으로 5단계(증상) 질문지를 동적으로 생성
   */
  public ChatBotMessage createSymptomMessage(Set<DisorderType> disorders) {
    // 4단계에서 선택한 질환(disorders)에 해당하는 증상들만 가져오기
    Set<SymptomType> symptoms = SymptomType.getByDisorderTypes(disorders);

    List<ButtonOption> options = symptoms.stream()
        .map(s -> ButtonOption.builder()
            .label(s.getDescription())
            .value(s.name())
            .isMultiSelect(true)
            .build())
        .collect(Collectors.toList());

    return ChatBotMessage.builder()
        .content("주로 힘들어 하는 일은 어떤건가요? 모리가 참고해서 도와줄게요")
        .type(MessageType.OPTION)
        .options(options)
        .build();
  }

  /**
   * 긍정/괜찮음 응답(고정 멘트)을 생성
   */
  public ChatBotMessage createPositiveResponseMessage(Set<EmotionType> emotions) {
    String content;
    if (emotions.size() == 1) {
      EmotionType emotion = emotions.iterator().next();
      switch (emotion) {
        case EXCITED: content = "무언가 기대되는 일이 있었나 봐요! 그 에너지, 좋아요 😆"; break;
        case JOY: content = "즐거운 순간이 있었군요. 그 기분 오래오래 간직해요 😊"; break;
        case PROUD: content = "오늘 스스로에게 칭찬해줄 일이 있었나 봐요! 정말 잘했어요 👏"; break;
        case HAPPY: content = "행복하다고 느껴지는 순간, 너무 소중하죠. 지금 이 마음을 기억해요 💛"; break;
        case FLUTTER: content = "마음이 간질간질, 좋은 일이 기다리고 있나 봐요! 설렘은 삶의 활력소예요 🌸"; break;
        case SO_SO: content = "큰 감정 변화는 없지만, 이런 날도 충분히 괜찮아요. 그냥 있는 그대로의 하루도 소중해요 🍃"; break;
        default: content = "긍정적인 감정을 느끼셨군요! 좋아요.";
      }
    } else if (emotions.size() == 2) {
      Map<Set<EmotionType>, String> combinationMessages = Map.ofEntries(
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.JOY), "신나고 즐거운 하루였네요! 이런 기분이 오래오래 이어졌으면 좋겠네요. 😄🎉"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.PROUD), "신나고 뿌듯한 하루를 보내셨네요. 오늘의 성취가 모리도 뿌듯하게 느껴지네요. 😆👏"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.HAPPY), "신나고 행복한 하루였네요. 좋은 일이 가득해서 저도 기분이 좋아지네요. 😊💛"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.FLUTTER), "신나고 설레는 하루였네요. 앞으로도 기대되는 일이 많으시길 바랄게요. 💫🌸"),
          Map.entry(Set.of(EmotionType.EXCITED, EmotionType.SO_SO), "신나는 순간도 있었고, 평범한 시간도 있었네요. 여러 감정이 어우러진 하루였던 것 같네요.🎭"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.PROUD), "즐겁고 뿌듯한 하루를 보내셨네요. 오늘의 좋은 기억이 오래 남았으면 해요. 😊👏"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.HAPPY), "즐거움과 행복이 함께한 하루였네요. 저도 덩달아 미소가 지어지네요. 😄💛"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.FLUTTER), "즐겁고 설레는 하루였네요. 새로운 시작이나 만남이 있었던 걸까요? 앞으로도 좋은 일이 가득하길 바랄게요. 🌟😊"),
          Map.entry(Set.of(EmotionType.JOY, EmotionType.SO_SO), "즐거운 순간도 있었고, 평범한 시간도 있었네요. 그런 하루도 충분히 의미 있네요. 🍃🙂"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.HAPPY), "뿌듯함과 행복이 함께한 하루였네요. 오늘의 성취가 큰 기쁨이 되었겠어요. 👏😊"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.FLUTTER), "뿌듯하고 설레는 하루였네요. 앞으로도 좋은 변화가 이어지길 바랄게요. 🌱💫"),
          Map.entry(Set.of(EmotionType.PROUD, EmotionType.SO_SO), "뿌듯한 순간과 평범한 시간이 함께한 하루였네요. 그런 균형이 참 소중하네요. ⚖️🍀"),
          Map.entry(Set.of(EmotionType.HAPPY, EmotionType.FLUTTER), "행복하고 설레는 하루였네요. 좋은 일이 곧 찾아올 것 같은 느낌이네요. 💛🌸"),
          Map.entry(Set.of(EmotionType.HAPPY, EmotionType.SO_SO), "행복한 순간도 있었고, 평범한 시간도 있었네요. 오늘 하루도 잘 보내셨네요. 🌤️🙂"),
          Map.entry(Set.of(EmotionType.FLUTTER, EmotionType.SO_SO), "설레는 순간도 있었고, 평범한 시간도 있었네요. 다양한 감정이 어우러진 하루였던 것 같네요. 🎈🍃")
      );
      content = combinationMessages.getOrDefault(emotions, "긍정적인 감정들이 함께했네요. 멋진 하루예요! 🌟");
    } else {
      content = "오늘 기분이 좋으셨군요!";
    }
    return ChatBotMessage.builder().content(content).type(MessageType.TEXT).build();
  }

  public ChatBotMessage createActionProposeMessage(String nickname) {
    return ChatBotMessage.builder()
        .content("그 상황에서 마음이 많이 복잡하고 힘들었겠어요. 다시 마주해야 한다고 생각하니 불안한 감정이 드는 게 정말 자연스러운 일이에요. 모리가 " + nickname + "님의 마음을 진정시키는데 도움이 될 수 있는 방법을 추천해도 될까요?")
        .type(MessageType.OPTION)
        .options(Arrays.asList(
            ButtonOption.builder().label("응, 뭔데?").value("YES_PROPOSE").build(),
            ButtonOption.builder().label("아니 혼자 진정하고 싶어").value("NO_PROPOSE").build()
        ))
        .build();
  }

  public ChatBotMessage createAloneComfortMessage(ChatSession session, String nickname) {
    String userSituation = session.getTemporaryData("userSituation");
    String selectedEmotions = session.getTemporaryData("selectedEmotions");

    // TODO: GPT 호출하여 userSituation과 selectedEmotions 기반으로 '상황에 따른 위로' 메시지 생성
    String gptComfortMessage = "(GPT가 생성한 위로 메시지)"; // 임시

    String finalMessage = "알겠어요. 지금은 혼자 생각을 정리하고 싶은 마음이 클 수도 있겠네요. 괜찮아요, 꼭 바로 뭔가 해결하려고 하지 않아도 돼요. "
        + gptComfortMessage + " "
        + "필요할 때 언제든 말 걸어줘요. 모리는 항상 " + nickname + "님 편이에요.";

    return ChatBotMessage.builder()
        .content(finalMessage)
        .type(MessageType.TEXT) // 텍스트만 보내고 종료
        .build();
  }

  public ChatBotMessage createSkillSelectMessage(ChatSession session) {
    String userSituation = session.getTemporaryData("userSituation");
    String selectedEmotions = session.getTemporaryData("selectedEmotions");

    // TODO: 1. GPT 호출하여 userSituation/selectedEmotions 기반으로 가장 적합한 skill_name 추천받기 (1개)
    String recommendedSkillName = "(GPT 추천 스킬 이름)"; // 임시 (예: "일단 멈추고 한숨 돌리기🛑")
    String recommendedSkillValue = "(GPT 추천 스킬 ID)"; // 임시 (예: "distress-001")

    // TODO: 2. (선택) 행동 지침 DB에서 추가적인 skill_name 몇 개 더 가져오기 (총 4개가 되도록)
    List<ButtonOption> options = Arrays.asList( // 임시 버튼 (실제로는 GPT 결과 + DB 조회 결과로 채워야 함)
        ButtonOption.builder().label(recommendedSkillName).value(recommendedSkillValue).build(),
        ButtonOption.builder().label("다른 스킬 1").value("SKILL_ID_2").build(),
        ButtonOption.builder().label("다른 스킬 2").value("SKILL_ID_3").build(),
        ButtonOption.builder().label("다른 스킬 3").value("SKILL_ID_4").build()
    );

    String content = "좋아요, 그럼 지금 이 감정에 도움이 될 수 있는 방법들을 하나씩 소개해볼게요. "
        + "지금 감정에 도움이 될 수 있는 방법들을 소개했어요. 이 중에서 하나 골라 함께 해볼까요?";

    return ChatBotMessage.builder()
        .content(content)
        .type(MessageType.OPTION)
        .options(options)
        .build();
  }
}
