package com.xhzb.ai;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.service.ChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
