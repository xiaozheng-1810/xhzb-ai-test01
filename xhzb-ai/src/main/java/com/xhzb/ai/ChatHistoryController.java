package com.xhzb.ai;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.domain.vo.MessageVO;
import com.xhzb.service.ChatHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话历史控制器
 *
 * @Author mr.wu
 * @Date 2026-8-14 9:55
 */
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController extends BaseController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 获取历史会话ID
     * @return
     */
    @GetMapping
    public AjaxResult findChatIdList(){
        List<String> chatIdList = chatHistoryService.findChatIdList(SecurityUtils.getUserId().toString());
        return success(chatIdList);
    }

    /**
     * 获取聊天记录详情
     * @param chatId
     * @return
     */
    @GetMapping("/{chatId}")
    public AjaxResult findChatHistoryDetails(@PathVariable String chatId){
        List<MessageVO> messageVOList = chatHistoryService.findChatHistoryDetails(chatId);
        return success(messageVOList);
    }

    /**
     * 删除会话及聊天信息
     * @param chatId
     * @return
     */
    @DeleteMapping("/{chatId}")
    public AjaxResult deleteChatHistory(@PathVariable String chatId) {
        chatHistoryService.deleteChatHistory(chatId, SecurityUtils.getUserId().toString());
        return success();
    }
}
