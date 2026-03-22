package com.campus.backend.ws;

import com.campus.backend.dto.ChatMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket 处理器：维护「用户ID → WebSocket 会话」的映射，并负责服务端主动推送聊天消息。
 *
 * <p>前端在建立连接时需要带上查询参数 userId，例如：<code>ws://host/ws/chat?userId=1</code>，
 * 这里会从 URL 中解析 userId 并缓存对应的 {@link WebSocketSession}，供后续精准推送。</p>
 *
 * <p>当前实现只用于服务端主动推送，客户端发来的文本消息会被忽略（可按需扩展为心跳 / 指令等）。</p>
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

  /**
   * 当有新的 WebSocket 连接建立时触发。
   * 这里尝试从连接 URL 中解析 userId，并将 userId 与 session 关联起来。
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = extractUserId(session.getUri());
    if (userId != null) {
      sessions.put(userId, session);
    }
  }

  /**
   * 当连接关闭时触发。
   * 需要把对应的 userId 从内存映射表中移除，避免内存泄漏。
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long userId = extractUserId(session.getUri());
    if (userId != null) {
      sessions.remove(userId);
    }
  }

  /**
   * 处理客户端发来的文本消息。
   * 当前项目中聊天消息走 HTTP + 服务端推送，因此这里不做任何处理，只保留扩展点。
   */
  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    // 可以在此处理客户端心跳或其他控制指令；目前聊天消息不从 WebSocket 入口进来。
  }

  /**
   * 将一条聊天消息推送给指定用户。
   *
   * @param userId 目标用户ID
   * @param item   要推送的聊天消息 DTO
   */
  public void pushToUser(Long userId, ChatMessageItem item) {
    if (userId == null || item == null) return;
    WebSocketSession session = sessions.get(userId);
    if (session == null || !session.isOpen()) return;
    try {
      String json = objectMapper.writeValueAsString(item);
      session.sendMessage(new TextMessage(json));
    } catch (IOException e) {
      // 忽略推送失败（如网络断开）
    }
  }

  /**
   * 从连接 URL 的查询参数中解析 userId。
   * 例如：/ws/chat?userId=1，则返回 1；解析失败或缺失时返回 null。
   */
  private Long extractUserId(URI uri) {
    if (uri == null) return null;
    try {
      Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
      String v = params.get("userId");
      if (v == null || v.isBlank()) return null;
      return Long.parseLong(v.trim());
    } catch (Exception e) {
      return null;
    }
  }
}

