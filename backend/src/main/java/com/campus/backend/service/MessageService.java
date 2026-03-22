package com.campus.backend.service;

import com.campus.backend.dto.ChatMessageItem;
import com.campus.backend.dto.ConversationItem;
import com.campus.backend.dto.SendMessageRequest;
import com.campus.backend.mapper.GoodsMessageMapper;
import com.campus.backend.mapper.TaskMessageMapper;
import com.campus.backend.model.GoodsMessage;
import com.campus.backend.model.TaskMessage;
import com.campus.backend.ws.ChatWebSocketHandler;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 聊天消息领域服务。
 *
 * <p>统一处理任务聊天（TASK）和二手交易聊天（GOODS）的会话列表、历史消息、已读状态以及发送逻辑，
 * 控制层只需要关心「当前用户 / 会话类型 / 业务ID / 对方用户」这些高层概念。</p>
 *
 * <p>发送消息时通过 MyBatis 将消息写入对应的消息表（t_task_message / t_goods_message），
 * 然后调用 {@link ChatWebSocketHandler} 将新消息实时推送给接收方。</p>
 */
@Service
public class MessageService {

  private final TaskMessageMapper taskMessageMapper;
  private final GoodsMessageMapper goodsMessageMapper;
  private final ChatWebSocketHandler chatWebSocketHandler;

  public MessageService(TaskMessageMapper taskMessageMapper,
      GoodsMessageMapper goodsMessageMapper,
      ChatWebSocketHandler chatWebSocketHandler) {
    this.taskMessageMapper = taskMessageMapper;
    this.goodsMessageMapper = goodsMessageMapper;
    this.chatWebSocketHandler = chatWebSocketHandler;
  }

  /**
   * 查询指定用户在某一会话类型下的会话列表。
   *
   * @param userId 当前用户ID
   * @param type   会话类型：TASK 或 GOODS（不区分大小写）
   * @return 会话列表，每一条对应一个任务或订单下与某个用户的最近一条对话及未读数
   */
  public List<ConversationItem> listConversations(Long userId, String type) {
    if ("TASK".equalsIgnoreCase(type)) {
      return taskMessageMapper.listTaskConversations(userId);
    }
    if ("GOODS".equalsIgnoreCase(type)) {
      return goodsMessageMapper.listGoodsConversations(userId);
    }
    throw new IllegalArgumentException("未知会话类型");
  }

  /**
   * 查询某个会话的历史消息列表。
   *
   * @param userId      当前用户ID（用于限定只查和自己收发相关的消息）
   * @param type        会话类型：TASK / GOODS
   * @param bizId       业务ID：任务ID 或 订单ID
   * @param otherUserId 对方用户ID
   * @param limit       最多返回多少条记录（为空时使用默认值，内部做上限保护）
   */
  public List<ChatMessageItem> listHistory(Long userId, String type, Long bizId,
      Long otherUserId, Integer limit) {
    int lim = limit == null ? 200 : Math.max(1, Math.min(limit, 500));
    if ("TASK".equalsIgnoreCase(type)) {
      return taskMessageMapper.listTaskHistory(userId, bizId, otherUserId, lim);
    }
    if ("GOODS".equalsIgnoreCase(type)) {
      return goodsMessageMapper.listGoodsHistory(userId, bizId, otherUserId, lim);
    }
    throw new IllegalArgumentException("未知会话类型");
  }

  /**
   * 将某个会话中「对方发给我」的消息标记为已读。
   *
   * @param userId      当前用户ID（消息接收方）
   * @param type        会话类型：TASK / GOODS
   * @param bizId       业务ID：任务ID 或 订单ID
   * @param otherUserId 对方用户ID（消息发送方）
   */
  @Transactional
  public void markRead(Long userId, String type, Long bizId, Long otherUserId) {
    if ("TASK".equalsIgnoreCase(type)) {
      taskMessageMapper.markTaskRead(userId, bizId, otherUserId);
      return;
    }
    if ("GOODS".equalsIgnoreCase(type)) {
      goodsMessageMapper.markGoodsRead(userId, bizId, otherUserId);
      return;
    }
    throw new IllegalArgumentException("未知会话类型");
  }

  /**
   * 发送一条聊天消息。
   *
   * <p>流程：</p>
   * <ol>
   *   <li>校验入参（会话类型、发送方、接收方、业务ID及内容合法性）；</li>
   *   <li>根据 type 分别写入 t_task_message 或 t_goods_message；</li>
   *   <li>构造统一的 {@link ChatMessageItem} DTO；</li>
   *   <li>通过 WebSocket 将该消息推送给接收方（若在线）。</li>
   * </ol>
   *
   * @param req 发送消息请求体
   * @return 新插入的消息对应的 DTO，用于前端直接追加到聊天窗口
   */
  @Transactional
  public ChatMessageItem send(SendMessageRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("请求不能为空");
    }
    if (req.getSenderId() == null || req.getReceiverId() == null || req.getBizId() == null) {
      throw new IllegalArgumentException("参数不完整");
    }
    String content = req.getContent() == null ? "" : req.getContent().trim();
    if (content.isEmpty()) {
      throw new IllegalArgumentException("消息内容不能为空");
    }
    if (content.length() > 1000) {
      throw new IllegalArgumentException("消息内容过长");
    }

    ChatMessageItem item = new ChatMessageItem();
    item.setType(req.getType() == null ? "" : req.getType().toUpperCase());
    item.setBizId(req.getBizId());
    item.setSenderId(req.getSenderId());
    item.setReceiverId(req.getReceiverId());
    item.setContent(content);
    item.setReadFlag(false);

    if ("TASK".equalsIgnoreCase(req.getType())) {
      TaskMessage m = new TaskMessage();
      m.setTaskId(req.getBizId());
      m.setSenderId(req.getSenderId());
      m.setReceiverId(req.getReceiverId());
      m.setContent(content);
      taskMessageMapper.insert(m);
      item.setId(m.getId());
    } else if ("GOODS".equalsIgnoreCase(req.getType())) {
      GoodsMessage m = new GoodsMessage();
      m.setOrderId(req.getBizId());
      m.setSenderId(req.getSenderId());
      m.setReceiverId(req.getReceiverId());
      m.setContent(content);
      goodsMessageMapper.insert(m);
      item.setId(m.getId());
    } else {
      throw new IllegalArgumentException("未知会话类型");
    }

    // 立即推送给接收方（若在线）
    chatWebSocketHandler.pushToUser(req.getReceiverId(), item);
    return item;
  }

  /**
   * 当前用户在所有消息（任务 + 商品）中的未读总数。
   */
  public int countUnreadTotal(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("用户ID不能为空");
    }
    int task = taskMessageMapper.countTaskUnread(userId);
    int goods = goodsMessageMapper.countGoodsUnread(userId);
    return task + goods;
  }

  /**
   * 清空某个会话下当前用户与指定对方之间的所有消息。
   *
   * @param userId      当前用户ID
   * @param type        会话类型：TASK / GOODS
   * @param bizId       任务ID或订单ID
   * @param otherUserId 对方用户ID
   */
  @Transactional
  public void clearConversation(Long userId, String type, Long bizId, Long otherUserId) {
    if (userId == null || bizId == null || otherUserId == null) {
      throw new IllegalArgumentException("参数不完整");
    }
    if ("TASK".equalsIgnoreCase(type)) {
      taskMessageMapper.deleteTaskConversation(userId, bizId, otherUserId);
      return;
    }
    if ("GOODS".equalsIgnoreCase(type)) {
      goodsMessageMapper.deleteGoodsConversation(userId, bizId, otherUserId);
      return;
    }
    throw new IllegalArgumentException("未知会话类型");
  }
}

