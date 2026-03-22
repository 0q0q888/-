package com.campus.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数的数据传输对象（DTO）。
 * 说明：
 * - 用于承接前端发送过来的 JSON 登录请求体。
 * - 搭配 @Valid 注解，在 Controller 中进行参数校验。
 */
public class LoginRequest {

  /** 登录用户名，对应前端输入的用户名 */
  @NotBlank(message = "用户名不能为空")
  private String username;

  /** 登录密码，对应前端输入的密码 */
  @NotBlank(message = "密码不能为空")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

