package com.xhzb.service;

import com.xhzb.domain.vo.MessageVO;

import java.util.List;

/**
 * 会话历史处理接口类
 *
 * @Author mr.wu
 * @Date 2026-8-14 9:49
 */
public interface ChatHistoryService {


    /**
     * 保存用户的会话ID
     * @param userId
     * @param chatId
     */
    void saveHistory(String userId, String chatId);

    List<String> findChatIdList(String string);

    List<MessageVO> findChatHistoryDetails(String chatId);

    void deleteChatHistory(String chatId, String string);
}