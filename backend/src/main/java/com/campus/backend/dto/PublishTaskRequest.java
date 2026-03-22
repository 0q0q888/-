package com.campus.backend.dto;

import java.time.LocalDateTime;

/**
 * 发布任务请求。
 */
public class PublishTaskRequest {

  private String title;
  private String description;
  private Double rewardAmount;
  private LocalDateTime deadlineTime;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Double getRewardAmount() {
    return rewardAmount;
  }

  public void setRewardAmount(Double rewardAmount) {
    this.rewardAmount = rewardAmount;
  }

  public LocalDateTime getDeadlineTime() {
    return deadlineTime;
  }

  public void setDeadlineTime(LocalDateTime deadlineTime) {
    this.deadlineTime = deadlineTime;
  }
}
