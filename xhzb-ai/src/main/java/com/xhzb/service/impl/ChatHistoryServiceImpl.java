package com.xhzb.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.xhzb.domain.vo.MessageVO;
import com.xhzb.service.ChatHistoryService;
import com.xhzb.domain.vo.Msg;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 会话历史处理业务实现类
 *
 * @Author mr.wu
 * @Date 2026-8-14 9:50
 */
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_TEMPLATE = "chat:history:%s";

    /**
     * 保存用户的会话ID
     *
     * @param userId
     * @param chatId
     */
    @Override
    public void saveHistory(String userId, String chatId) {
        String key = String.format(KEY_TEMPLATE,userId);
        redisTemplate.opsForSet().add(key,chatId);
    }

    /**
     * 获取历史会话ID
     *
     * @return
     */
    @Override
    public List<String> findChatIdList(String userId) {
        String key = String.format(KEY_TEMPLATE,userId);
        Set<String> chatIdSet = redisTemplate.opsForSet().members(key);
        if (CollUtil.isEmpty(chatIdSet)) {
            return List.of();
        }

        List<String> chatIdList = chatIdSet.stream()
                .sorted(Comparator.comparing(String::toString).reversed()) //对会话ID进行降序排序
                .toList();

        return chatIdList;
    }

    @Autowired
    private ChatMemory chatMemory;

    /**
     * 获取聊天记录详情
     *
     * @param chatId
     * @return
     */
    @Override
    public List<MessageVO> findChatHistoryDetails(String chatId) {
        //从会话记忆处理类中获取会话ID对应的历史对话消息列表
        List<Message> messageList = chatMemory.get(chatId);
        if (CollUtil.isEmpty(messageList)) {
            return List.of();
        }

        List<MessageVO> messageVOList = messageList.stream()
                .map(MessageVO::new) //将SpringAI消息转为MessageVO
                .toList();
        return messageVOList;
    }

    /**
     * 删除会话及聊天信息
     *
     * @param chatId
     */
    @Override
    public void deleteChatHistory(String chatId, String userId) {
        String key = String.format(KEY_TEMPLATE,userId);
        //从第一个缓存中删除用户对应的会话ID
        redisTemplate.opsForSet().remove(key,chatId);

        //从第二个缓存中删除会话ID对应的全部消息
        chatMemory.clear(chatId);
    }
}
