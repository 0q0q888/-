package com.campus.backend.service;

import com.campus.backend.dto.AiChatResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * AI 客服业务服务，负责调用 deepseekapi 并返回可展示文本。
 */
@Service
public class AiCustomerService {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${deepseek.base-url:http://localhost:9000}")
  private String deepseekBaseUrl;

  /**
   * 校验消息后调用模型接口。
   * 成功返回 `reply`，失败抛出参数异常。
   */
  public AiChatResponse chat(String message) {
    String content = message == null ? "" : message.trim();
    if (content.isEmpty()) {
      throw new IllegalArgumentException("message 不能为空");
    }

    // 不追加系统/角色等提示词：prompt 即为用户输入原文，模型输出原样返回前端。
    String prompt = content;
    String endpoint = normalizeBaseUrl(deepseekBaseUrl) + "/generate";

    Map<String, Object> reqBody = Map.of("prompt", prompt);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

    Map<String, Object> modelResp;
    try {
      modelResp = restTemplate.postForObject(endpoint, entity, Map.class);
    } catch (RestClientException ex) {
      throw new IllegalArgumentException("调用大模型失败: " + ex.getMessage(), ex);
    }

    String reply = extractReply(modelResp);
    if (reply == null || reply.isBlank()) {
      throw new IllegalArgumentException("大模型未返回有效回复");
    }

    AiChatResponse response = new AiChatResponse();
    response.setReply(reply);
    return response;
  }

  /**
   * 规范化模型服务地址，确保没有尾部斜杠。
   * 为空时使用默认本地地址。
   */
  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://localhost:9000";
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * 从模型响应中提取文本，优先 `generated_text`。
   * 同时兼容 OpenAI 风格的 `choices` 字段。
   */
  @SuppressWarnings("unchecked")
  private String extractReply(Map<String, Object> modelResp) {
    if (modelResp == null || modelResp.isEmpty()) {
      return "";
    }

    Object generated = modelResp.get("generated_text");
    if (generated instanceof String s && !s.isBlank()) {
      return s;
    }

    Object choicesObj = modelResp.get("choices");
    if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
      Object first = choices.get(0);
      if (first instanceof Map<?, ?> firstMapRaw) {
        Map<String, Object> firstMap = (Map<String, Object>) firstMapRaw;
        Object messageObj = firstMap.get("message");
        if (messageObj instanceof Map<?, ?> messageMapRaw) {
          Map<String, Object> messageMap = (Map<String, Object>) messageMapRaw;
          Object content = messageMap.get("content");
          if (content instanceof String s && !s.isBlank()) {
            return s;
          }
        }
        Object text = firstMap.get("text");
        if (text instanceof String s && !s.isBlank()) {
          return s;
        }
      }
    }
    return "";
  }
}

