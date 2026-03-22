package com.campus.backend.dto;

/**
 * YOLO 识别标题建议请求 DTO。
 *
 * <p>前端在卖出宝贝弹窗中点击“一键发布”时，会先上传商品图片，
 * 然后将图片的访问地址（例如 /uploads/goods/xxx.png）传给后端。</p>
 */
public class YoloTitleSuggestRequest {

  /** 商品主图的访问 URL，可以是相对路径（/uploads/xxx）或完整 http URL。 */
  private String imageUrl;

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
}

