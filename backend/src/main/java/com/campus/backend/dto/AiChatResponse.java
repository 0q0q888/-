package com.campus.backend.dto;

/**
 * AI 客服聊天响应 DTO。
 */
public class AiChatResponse {

  /** 大模型返回给前端的最终回复文本。 */
  private String reply;

  public String getReply() {
    return reply;
  }

  public void setReply(String reply) {
    this.reply = reply;
  }
}

