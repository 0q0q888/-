package com.campus.backend.dto;

/**
 * 发布二手商品的请求体，对应前端发布表单中的字段。
 */
public class CreateGoodsRequest {

  /** 商品标题（必填） */
  private String title;

  /** 商品描述（选填） */
  private String description;

  /** 价格（选填，非负，不填则显示暂无报价） */
  private Double price;

  /** 图片URL（必填，逗号分隔多张） */
  private String images;

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
}

