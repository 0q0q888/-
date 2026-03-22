package com.campus.backend.model;

import java.time.LocalDateTime;

/**
 * 二手商品实体类，对应数据库表 t_goods。
 * 字段说明：
 * - 物理字段与 t_goods 列一一对应（id、title、description、price、images、sellerId、status、createTime、updateTime）；
 * - sellerNickname 为通过关联 t_user.nickname 查询出的衍生字段，便于前端直接展示卖家昵称。
 */
public class Goods {

  /** 商品ID，对应 t_goods.id */
  private Long id;

  /** 商品标题，对应 t_goods.title */
  private String title;

  /** 商品描述，对应 t_goods.description */
  private String description;

  /** 价格，对应 t_goods.price */
  private Double price;

  /** 图片URL（逗号分隔），对应 t_goods.images */
  private String images;

  /** 卖家用户ID，对应 t_goods.seller_id */
  private Long sellerId;

  /** 卖家昵称（通过关联 t_user.nickname 获取，非 t_goods 物理字段） */
  private String sellerNickname;

  /** 商品状态：ON_SALE / SOLD / OFF_SHELF，对应 t_goods.status */
  private String status;

  /** 创建时间，对应 t_goods.create_time */
  private LocalDateTime createTime;

  /** 更新时间，对应 t_goods.update_time */
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

  public Double getPrice() {
    return price;
  }

  public void setPrice(Double price) {
    this.price = price;
  }

  public String getImages() {
    return images;
  }

  public void setImages(String images) {
    this.images = images;
  }

  public Long getSellerId() {
    return sellerId;
  }

  public void setSellerId(Long sellerId) {
    this.sellerId = sellerId;
  }

  public String getSellerNickname() {
    return sellerNickname;
  }

  public void setSellerNickname(String sellerNickname) {
    this.sellerNickname = sellerNickname;
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
}

