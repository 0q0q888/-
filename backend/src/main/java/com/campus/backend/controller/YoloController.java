package com.campus.backend.controller;

import com.campus.backend.dto.YoloTitleSuggestRequest;
import com.campus.backend.dto.YoloTitleSuggestResponse;
import com.campus.backend.service.YoloService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * YOLO 相关接口。
 *
 * <p>当前仅提供一个根据商品主图自动生成标题/描述建议的接口，
 * 用于“卖出宝贝”弹窗中的“一键发布”功能。</p>
 */
@RestController
@RequestMapping("/api/yolo")
public class YoloController {

  private final YoloService yoloService;

  public YoloController(YoloService yoloService) {
    this.yoloService = yoloService;
  }

  /**
   * 根据图片 URL 获取标题/描述建议。
   *
   * <p>前端请求示例：</p>
   * <pre>
   *   POST /api/yolo/goods/title-suggest
   *   body: { "imageUrl": "/uploads/goods/xxx.png" }
   * </pre>
   *
   * <p>本接口会将相对路径转换为完整的 http URL，然后转发给 Python YOLO 服务。</p>
   */
  @PostMapping("/goods/title-suggest")
  public ResponseEntity<YoloTitleSuggestResponse> suggestTitle(
      @RequestBody YoloTitleSuggestRequest request,
      HttpServletRequest servletRequest) {
    String imageUrl = request == null ? null : request.getImageUrl();
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("imageUrl 不能为空");
    }

    String absoluteUrl = imageUrl;
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
      String scheme = servletRequest.getScheme();
      String host = servletRequest.getServerName();
      int port = servletRequest.getServerPort();
      String base = scheme + "://" + host + (port == 80 || port == 443 ? "" : ":" + port);
      if (!imageUrl.startsWith("/")) {
        imageUrl = "/" + imageUrl;
      }
      absoluteUrl = base + imageUrl;
    }

    YoloTitleSuggestResponse resp = yoloService.suggestTitleByImageUrl(absoluteUrl);
    return ResponseEntity.ok(resp);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}

