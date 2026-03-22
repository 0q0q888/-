package com.campus.backend.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类。
 *
 * <p>启用 Spring WebSocket 功能，并将 {@link ChatWebSocketHandler} 注册到 <code>/ws/chat</code> 路径上，
 * 允许任意来源建立连接（方便开发与内网穿透场景）。</p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ChatWebSocketHandler chatWebSocketHandler;

  public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
    this.chatWebSocketHandler = chatWebSocketHandler;
  }

  /**
   * 注册 WebSocket 处理器。
   *
   * @param registry Spring 提供的注册器，用于配置路径、跨域策略等
   */
  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(chatWebSocketHandler, "/ws/chat")
        .setAllowedOriginPatterns("*");
  }
}

