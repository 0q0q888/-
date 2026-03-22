package com.campus.backend.controller;

import com.campus.backend.dto.AiChatRequest;
import com.campus.backend.dto.AiChatResponse;
import com.campus.backend.service.AiCustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 客服入口，接收前端消息并转给服务层处理。
 */
@RestController
@RequestMapping("/api/ai-service")
public class AiCustomerController {

  private final AiCustomerService aiCustomerService;

  public AiCustomerController(AiCustomerService aiCustomerService) {
    this.aiCustomerService = aiCustomerService;
  }

  /**
   * 接收聊天请求并返回 AI 回复。
   * 请求体只需要 `message` 字段。
   */
  @PostMapping("/chat")
  public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("请求体不能为空");
    }
    return ResponseEntity.ok(
        aiCustomerService.chat(request.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  /** 参数或调用异常统一返回 400。 */
  public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}

