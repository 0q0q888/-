package com.campus.backend.controller;

import com.campus.backend.dto.ChangePasswordRequest;
import com.campus.backend.dto.LoginRequest;
import com.campus.backend.dto.LoginResponse;
import com.campus.backend.dto.RegisterRequest;
import com.campus.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关的控制器，负责接收前端的 HTTP 请求并返回响应。
 * 说明：
 * - 当前提供 /api/auth/login 登录接口；
 * - 提供 /api/auth/register 注册普通用户接口；
 * - 使用 MVC 模式：Controller 调用 Service，Service 再调用 Mapper。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /** 认证业务逻辑服务对象，用于处理登录等操作 */
  private final AuthService authService;

  /**
   * 构造方法，用于注入 AuthService。
   *
   * @param authService 认证相关业务逻辑服务
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * 登录接口。
   *
   * 请求方式：POST
   * 请求路径：/api/auth/login
   * 请求体：JSON，对应 LoginRequest（包含 username 和 password）
   *
   * @param request 登录请求参数（使用 @Valid 进行参数非空校验）
   * @return 登录成功返回 200 OK + LoginResponse；失败时抛出异常并由统一异常处理返回 400
   */
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * 注册普通用户接口。
   *
   * 请求方式：POST
   * 请求路径：/api/auth/register
   * 请求体：JSON，对应 RegisterRequest（包含 username、nickname、password）
   *
   * 说明：
   * - 该接口只用于注册普通用户，角色固定为 USER；
   * - 当用户名已存在或参数不合法时，会抛出 IllegalArgumentException 或校验异常，被统一转换为 400 响应。
   *
   * @param request 注册请求参数（使用 @Valid 进行参数非空等校验）
   * @return 注册成功返回 200 OK + LoginResponse（包含基本用户信息）
   */
  @PostMapping("/register")
  public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
    LoginResponse response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @RequestParam("userId") Long userId,
      @RequestBody ChangePasswordRequest request) {
    authService.changePassword(userId, request);
    return ResponseEntity.ok().build();
  }

  /**
   * 处理登录过程中抛出的 IllegalArgumentException 异常。
   * 说明：
   * - 用于将业务异常转换为 HTTP 400 响应，并在响应体中返回错误信息。
   *
   * @param ex 登录流程中抛出的非法参数异常
   * @return 包含错误信息的 400 Bad Request 响应
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }


}

