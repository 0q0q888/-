package com.campus.backend.service;

import com.campus.backend.dto.YoloTitleSuggestResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 封装与 YOLO 推理服务交互的逻辑。
 *
 * <p>约定 Python 推理服务提供接口：</p>
 *
 * <pre>
 *   POST {yolo.base-url}/goods/title-suggest
 *   body: { "imageUrl": "http://localhost:8080/uploads/goods/xxx.png" }
 *   返回: { "titleSuggest": "...", "descTemplate": "..." }
 * </pre>
 */
@Service
public class YoloService {

  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * YOLO 推理服务的基础地址，可在 application.yml 中通过 yolo.base-url 配置。
   * 默认使用 http://localhost:8001。
   */
  @Value("${yolo.base-url:http://localhost:8001}")
  private String yoloBaseUrl;

  /**
   * 调用 YOLO 推理服务，根据图片 URL 获取一个标题/描述建议。
   *
   * @param absoluteImageUrl 可直接在 Python 服务中访问的图片完整 URL
   * @return 标题/描述建议
   */
  public YoloTitleSuggestResponse suggestTitleByImageUrl(String absoluteImageUrl) {
    if (absoluteImageUrl == null || absoluteImageUrl.isBlank()) {
      throw new IllegalArgumentException("imageUrl 不能为空");
    }

    String endpoint = yoloBaseUrl.endsWith("/")
        ? yoloBaseUrl + "goods/title-suggest"
        : yoloBaseUrl + "/goods/title-suggest";

    Map<String, Object> body = new HashMap<>();
    body.put("imageUrl", absoluteImageUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<YoloTitleSuggestResponse> response =
          restTemplate.postForEntity(endpoint, entity, YoloTitleSuggestResponse.class);
      YoloTitleSuggestResponse result = response.getBody();
      if (result == null) {
        throw new IllegalArgumentException("YOLO 服务未返回结果");
      }
      return result;
    } catch (RestClientException ex) {
      throw new IllegalArgumentException("调用 YOLO 服务失败: " + ex.getMessage(), ex);
    }
  }
}

