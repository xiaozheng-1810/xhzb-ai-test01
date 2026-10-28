package com.xhzb.nursing.config;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthAIConfig {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Bean
    public ChatClient healthChatClient() {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个资深的健康评估专家,专门根据用户提交的相关数据评估老人的健康情况")
                .build();
    }
}
