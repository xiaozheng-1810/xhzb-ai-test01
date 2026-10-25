package com.xhzb.ai;


import jakarta.annotation.Resource;
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

    //注入ChatClient聊天客户端对象
    @Autowired
    @Qualifier("openai")
    private ChatClient chatClient;

  /*  @PostMapping("/chat")
    public String chatting(String prompt, String chatId) {
        return chatClient.prompt(prompt).call().content();
    }*/
    @PostMapping("/chat")
    public Flux<String> chatting(String prompt, String chatId) {
        Flux<String> content = chatClient
                .prompt(prompt)
                .stream()
                .content();
        return content;
    }


}
