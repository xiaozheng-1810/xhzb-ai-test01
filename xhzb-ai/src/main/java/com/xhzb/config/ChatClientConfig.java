package com.xhzb.config;

import com.xhzb.constants.Constants;
import com.xhzb.tool.NursingProjectTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
    聊天的客户端有很多配置不适合在yml中
    比如:
        1.系统提示词很长
        2.模型的对接方式, 到底是本地的,还是openAI的, 还是其他方法
            对接不同的方式需要创建不同的对象才行!!!!!!!!
       ...
    综合以上因素, 这个客户端必须由我们自己创建而且还需要放入ioc容器中
 */
@Configuration // 标注这是一个配置类
public class ChatClientConfig {

    //注入OpenAi读取配置文件信息内容整合模型参数对象
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private DeepSeekChatModel deepSeekChatModel;

    @Autowired
    private NursingProjectTool nursingProjectTool;


    @Bean("openai")
    public ChatClient openAIChatClient( ) {
        // 构建聊天客户端, 必须有模型的参数, 才能构建, 还可以设置其他的重要的参数
        return
                ChatClient.builder(openAiChatModel)
                        .defaultSystem(Constants.PROMPT) // 配置系统提示词
                        .defaultTools(nursingProjectTool)// 配置技能
                        .defaultAdvisors(new SimpleLoggerAdvisor())  //添加默认的advisor 记录日志
                        .build();
    }

    @Bean("ollamaai")
    public ChatClient ollamaChatClient() {
        // 构建聊天客户端, 必须有模型的参数, 才能构建, 还可以设置其他的重要的参数
        return
                ChatClient.builder(ollamaChatModel)
                        .defaultSystem(Constants.PROMPT)// 配置系统提示词
                        .defaultTools(nursingProjectTool)// 配置技能
                        .defaultAdvisors(new SimpleLoggerAdvisor())  //添加默认的advisor 记录日志
                        .build();
    }

    @Bean("deepseekai")
    public ChatClient deepseekChatClient(ChatMemory chatMemory) {
        // 构建聊天客户端, 必须有模型的参数, 才能构建, 还可以设置其他的重要的参数
        return
                ChatClient.builder(deepSeekChatModel)
                        .defaultSystem(Constants.PROMPT)// 配置系统提示词
                        .defaultTools(nursingProjectTool)// 配置技能
                        .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())  //添加默认的advisor 记录日志
                        .build();
    }
}