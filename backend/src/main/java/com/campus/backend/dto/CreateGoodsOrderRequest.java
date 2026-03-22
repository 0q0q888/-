package com.campus.backend.dto;

/**
 * 创建二手交易订单的请求体。
 * 前端在“立即购买”时会传入 goodsId 和 buyerId。
 */
public class CreateGoodsOrderRequest {

  /** 对应的商品ID */
  private Long goodsId;

  /** 买家用户ID */
  private Long buyerId;

  public Long getGoodsId() {
    return goodsId;
  }

  public void setGoodsId(Long goodsId) {
    this.goodsId = goodsId;
  }

  public Long getBuyerId() {
    return buyerId;
  }

  public void setBuyerId(Long buyerId) {
    this.buyerId = buyerId;
  }
}

