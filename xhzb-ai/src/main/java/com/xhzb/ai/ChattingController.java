package com.xhzb.ai;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.service.ChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class ChattingController {
    @Autowired
    private ChatHistoryService chatHistoryService;

    //注入ChatClient聊天客户端对象
    @Autowired
    @Qualifier("deepseekai")
    private ChatClient chatClient;

    /*  @PostMapping("/chat")
      public String chatting(String prompt, String chatId) {
          return chatClient.prompt(prompt).call().content();
      }*/

//    原始方式实现上下文存储
/*    Map<String, List<Message>> messageCache = new HashMap<>();
    @PostMapping("/newChat")
    public String chatting(String prompt, String chatId) {
        // 调用模型之前先查询出来历史会话
        List<Message> historyMessageList = messageCache.get(chatId);
        // 判断历史会话是否存在
        if (historyMessageList == null) {
            historyMessageList = new ArrayList<>();
        }
        // 把本次的提示词包装为用户消息并存入历史对话
        Message userMessage = new UserMessage(prompt);
        historyMessageList.add(userMessage);
        // 把历史信息存为新的提示词
        Prompt targetPrompt = new Prompt(historyMessageList);
        String llmResponse = chatClient.prompt(targetPrompt)
                .call()
                .content();
        // 把模型的响应包装为系统消息并存入历史对话
        Message assistantMessage = new AssistantMessage(llmResponse);
        historyMessageList.add(assistantMessage);
        // 把更新后的历史对话存入缓存
        messageCache.put(chatId, historyMessageList);
        return llmResponse;
    }*/

    @PostMapping("/chat")
    public Flux<String> chatting(String prompt, String chatId) {

        if(StrUtil.isBlank(chatId) || "null".equals(chatId.trim())) {
            throw new BaseException("请先新建会话");
        }

        chatHistoryService.saveHistory(SecurityUtils.getUserId().toString(), chatId);

        Flux<String> content = chatClient
                .prompt(prompt)
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
        return content;
    }


}
