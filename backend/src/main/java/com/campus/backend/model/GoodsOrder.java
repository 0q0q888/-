package com.campus.backend.model;

import java.time.LocalDateTime;

/**
 * 二手交易订单实体类，对应数据库表 t_goods_order。
 * 字段说明：
 * - 物理字段与 t_goods_order 列一一对应；
 * - goodsTitle 为通过关联 t_goods.title 查询出的衍生字段，方便前端直接展示订单对应的商品名称。
 */
public class GoodsOrder {

  /** 订单ID，对应 t_goods_order.id */
  private Long id;

  /** 对应商品ID，对应 t_goods_order.goods_id */
  private Long goodsId;

  /** 买家用户ID，对应 t_goods_order.buyer_id */
  private Long buyerId;

  /** 卖家用户ID，对应 t_goods_order.seller_id */
  private Long sellerId;

  /** 成交价格（冗余保存），对应 t_goods_order.price */
  private Double price;

  /** 订单状态：CREATED / PAID / FINISHED / CANCELLED，对应 t_goods_order.status */
  private String status;

  /** 创建时间，对应 t_goods_order.create_time */
  private LocalDateTime createTime;

  /** 更新时间，对应 t_goods_order.update_time */
  private LocalDateTime updateTime;

  /** 关联的商品标题（通过 t_goods.title 关联查询，非物理字段） */
  private String goodsTitle;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long sellerId) {
    this.sellerId = sellerId;
  }

  public Double getPrice() {
    return price;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public String getGoodsTitle() {
    return goodsTitle;
  }

  public void setGoodsTitle(String goodsTitle) {
    this.goodsTitle = goodsTitle;
  }
}

