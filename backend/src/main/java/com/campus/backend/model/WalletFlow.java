package com.campus.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水实体，对应 t_wallet_flow。
 * amount：收入为正，支出为负。
 */
public class WalletFlow {

  private Long id;
  private Long userId;
  private BigDecimal amount;
  private String type;   // RECHARGE, INCOME, EXPENSE
  private String bizType; // TASK_REWARD, GOODS_TRADE, OTHER
  private Long bizId;
  private String remark;
  private LocalDateTime createTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public Long getBizId() {
    return bizId;
  }

  public void setBizId(Long bizId) {
    this.bizId = bizId;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
}
