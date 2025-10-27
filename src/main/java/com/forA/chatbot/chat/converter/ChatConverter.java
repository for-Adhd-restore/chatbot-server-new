package com.forA.chatbot.chat.converter;

import com.forA.chatbot.chat.dto.ChatResponse;
import com.forA.chatbot.chat.dto.ChatResponse.ChatMessageDto;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatConverter {
  public ChatResponse.ChatBotMessage convertLatestHistoryToBotMessage(List<ChatMessageDto> history) {
    if (history == null || history.isEmpty()) {
      throw new IllegalArgumentException("history is null or empty");
    }
    ChatResponse.ChatMessageDto latestMessage = history.stream()
        .max(Comparator.comparing(ChatResponse.ChatMessageDto::getSentAt))
        .get();
    return ChatResponse.ChatBotMessage.builder()
        .content(latestMessage.getContent())
        .type(latestMessage.getType())
        .options(latestMessage.getOptions())
        .build();
  }

}
