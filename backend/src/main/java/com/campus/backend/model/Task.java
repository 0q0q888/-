package com.campus.backend.model;

import java.time.LocalDateTime;

/**
 * 任务实体类，对应数据库表 t_task。
 * 说明：
 * - 用于承载任务大厅相关的任务数据。
 * - 字段名与 t_task 列一一对应或做了简单驼峰映射。
 */
public class Task {

  /** 任务ID，对应 t_task.id */
  private Long id;

  /** 任务标题，对应 t_task.title */
  private String title;

  /** 任务描述，对应 t_task.description */
  private String description;

  /** 酬劳金额，对应 t_task.reward_amount */
  private Double rewardAmount;

  /** 发布人用户ID，对应 t_task.publisher_id */
  private Long publisherId;

  /** 接单人用户ID，对应 t_task.receiver_id */
  private Long receiverId;

  /** 发布人昵称（通过关联 t_user.nickname 获取，非 t_task 物理字段） */
  private String publisherNickname;

  /** 接单人昵称（通过关联 t_user.nickname 获取，非 t_task 物理字段） */
  private String receiverNickname;

  /** 任务状态：OPEN/TAKEN/FINISHED/CANCELLED，对应 t_task.status */
  private String status;

  /** 接单截止时间，过期后不可接单，对应 t_task.deadline_time */
  private LocalDateTime deadlineTime;

  /** 创建时间，对应 t_task.create_time */
  private LocalDateTime createTime;

  /** 更新时间，对应 t_task.update_time */
  private LocalDateTime updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public Long getPublisherId() {
    return publisherId;
  }

  public void setPublisherId(Long publisherId) {
    this.publisherId = publisherId;
  }

  public Long getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(Long receiverId) {
    this.receiverId = receiverId;
  }

  public String getPublisherNickname() {
    return publisherNickname;
  }

  public void setPublisherNickname(String publisherNickname) {
    this.publisherNickname = publisherNickname;
  }

  public String getReceiverNickname() {
    return receiverNickname;
  }

  public void setReceiverNickname(String receiverNickname) {
    this.receiverNickname = receiverNickname;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getDeadlineTime() {
    return deadlineTime;
  }

  public void setDeadlineTime(LocalDateTime deadlineTime) {
    this.deadlineTime = deadlineTime;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }
}

