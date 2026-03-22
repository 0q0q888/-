package com.campus.backend.controller;

import com.campus.backend.dto.ChatMessageItem;
import com.campus.backend.dto.ConversationItem;
import com.campus.backend.dto.SendMessageRequest;
import com.campus.backend.service.MessageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  /**
   * 会话列表。
   * GET /api/messages/conversations?userId=1&type=TASK|GOODS
   */
  @GetMapping("/conversations")
  public ResponseEntity<List<ConversationItem>> conversations(
      @RequestParam("userId") Long userId,
      @RequestParam("type") String type) {
    return ResponseEntity.ok(messageService.listConversations(userId, type));
  }

  /**
   * 历史消息。
   * GET /api/messages/history?userId=1&type=TASK&bizId=12&otherUserId=2&limit=200
   */
  @GetMapping("/history")
  public ResponseEntity<List<ChatMessageItem>> history(
      @RequestParam("userId") Long userId,
      @RequestParam("type") String type,
      @RequestParam("bizId") Long bizId,
      @RequestParam("otherUserId") Long otherUserId,
      @RequestParam(value = "limit", required = false) Integer limit) {
    return ResponseEntity.ok(messageService.listHistory(userId, type, bizId, otherUserId, limit));
  }

  /**
   * 标记已读。
   * POST /api/messages/read?userId=1&type=TASK&bizId=12&otherUserId=2
   */
  @PostMapping("/read")
  public ResponseEntity<Void> markRead(
      @RequestParam("userId") Long userId,
      @RequestParam("type") String type,
      @RequestParam("bizId") Long bizId,
      @RequestParam("otherUserId") Long otherUserId) {
    messageService.markRead(userId, type, bizId, otherUserId);
    return ResponseEntity.ok().build();
  }

  /**
   * 发送消息（REST 发送，WebSocket 推送给接收方）。
   * POST /api/messages/send
   */
  @PostMapping("/send")
  public ResponseEntity<ChatMessageItem> send(@RequestBody SendMessageRequest request) {
    return ResponseEntity.ok(messageService.send(request));
  }

  /**
   * 当前用户的未读消息总数（任务 + 商品）。
   * GET /api/messages/unread-total?userId=1
   */
  @GetMapping("/unread-total")
  public ResponseEntity<Integer> unreadTotal(@RequestParam("userId") Long userId) {
    return ResponseEntity.ok(messageService.countUnreadTotal(userId));
  }

  /**
   * 清空某个会话下当前用户与指定对方之间的所有消息。
   * POST /api/messages/clear?userId=1&type=TASK&bizId=12&otherUserId=2
   */
  @PostMapping("/clear")
  public ResponseEntity<Void> clearConversation(
      @RequestParam("userId") Long userId,
      @RequestParam("type") String type,
      @RequestParam("bizId") Long bizId,
      @RequestParam("otherUserId") Long otherUserId) {
    messageService.clearConversation(userId, type, bizId, otherUserId);
    return ResponseEntity.ok().build();
  }
}

