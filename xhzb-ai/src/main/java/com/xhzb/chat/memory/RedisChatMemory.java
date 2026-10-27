package com.xhzb.chat.memory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.domain.vo.Msg;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 自定义基于Redis操作的会话记忆处理类
 *
 * @Author mr.wu
 * @Date 2026-8-14 8:52
 */
@Component
public class RedisChatMemory implements ChatMemory {


    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_TEMPLATE = "chat:memory:%s";


    /**
     * 根据会话ID存储消息列表到Redis的List数据类型中
     *
     * @param conversationId
     * @param messages
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        // 判断消息列表是否为空
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        // 处理SpringAI的每条消息：先转Msg再转JSON字符串
//        List<String> msgList = new ArrayList<>();
//        for (Message message : messages) {
//            // 将SpringAI消息转为自定义消息
//            Msg msg = new Msg(message);
//
//            // 将自定义消息转为JSON字符串
//            String msgStr = JSONUtil.toJsonStr(msg);
//            msgList.add(msgStr);
//        }
        List<String> msgList = messages.stream()
                .map(Msg::new)// 将SpringAI消息转为自定义消息
                .map(x -> JSONUtil.toJsonStr(x)) // 将自定义消息转为JSON字符串
                .toList(); //将所有JSON字符串收集到集合中

        // 存储消息到Redis的List类型中
        String key = String.format(KEY_TEMPLATE,conversationId);
        redisTemplate.opsForList().leftPushAll(key,msgList);
    }

    /**
     * 根据会话ID从Redis的List数据类型中获取消息列表
     *
     * @param conversationId
     */
    @Override
    public List<Message> get(String conversationId) {
        String key = String.format(KEY_TEMPLATE,conversationId);
        List<String> msgList = redisTemplate.opsForList().range(key, 0, -1);
        //判断redis中数据是否为空
        if (CollUtil.isEmpty(msgList)) {
            return List.of();
        }

        //处理JSON字符串的每条消息：先转Msg再转SpringAI消息
        List<Message> messageList = msgList.stream()
                .map(x -> JSONUtil.toBean(x, Msg.class)) // JSON字符串消息转为自定义消息
                .sorted(Comparator.comparing(Msg::getCreateTime)) // 对自定义消息按照时间由旧及新排序
                .map(Msg::toMessage) //将自定义消息转为SpringAI消息
                .toList(); //将所有SpringAI消息收集到集合中

        return messageList;
    }

    /**
     * 根据会话ID删除消息
     *
     * @param conversationId
     */
    @Override
    public void clear(String conversationId) {
        String key = String.format(KEY_TEMPLATE,conversationId);
        redisTemplate.delete(key);
    }
}