package com.campus.backend.dto;

/**
 * 发送消息请求（任务/商品）。
 */
public class SendMessageRequest {

  private String type; // TASK / GOODS
  private Long bizId; // task_id / goods_order_id
  private Long senderId;
  private Long receiverId;
  private String content;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getBizId() {
    return bizId;
  }

  public void setBizId(Long bizId) {
    this.bizId = bizId;
  }

  public Long getSenderId() {
    return senderId;
  }

  public void setSenderId(Long senderId) {
    this.senderId = senderId;
  }

  public Long getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(Long receiverId) {
    this.receiverId = receiverId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}

