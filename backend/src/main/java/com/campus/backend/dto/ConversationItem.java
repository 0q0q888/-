package com.campus.backend.dto;

import java.time.LocalDateTime;

/**
 * 会话列表项（任务/商品）。
 */
public class ConversationItem {

  private String type; // TASK / GOODS
  private Long bizId; // task_id or goods_order_id
  private String title; // task.title or goods.title

  private Long otherUserId;
  private String otherNickname;
  private String otherAvatarUrl;

  private String lastContent;
  private LocalDateTime lastTime;
  private Integer unreadCount;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Long getOtherUserId() {
    return otherUserId;
  }

  public void setOtherUserId(Long otherUserId) {
    this.otherUserId = otherUserId;
  }

  public String getOtherNickname() {
    return otherNickname;
  }

  public void setOtherNickname(String otherNickname) {
    this.otherNickname = otherNickname;
  }

  public String getOtherAvatarUrl() {
    return otherAvatarUrl;
  }

  public void setOtherAvatarUrl(String otherAvatarUrl) {
    this.otherAvatarUrl = otherAvatarUrl;
  }

  public String getLastContent() {
    return lastContent;
  }

  public void setLastContent(String lastContent) {
    this.lastContent = lastContent;
  }

  public LocalDateTime getLastTime() {
    return lastTime;
  }

  public void setLastTime(LocalDateTime lastTime) {
    this.lastTime = lastTime;
  }

  public Integer getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(Integer unreadCount) {
    this.unreadCount = unreadCount;
  }
}

