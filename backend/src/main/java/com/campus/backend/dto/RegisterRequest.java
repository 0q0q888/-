package com.campus.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求参数的数据传输对象（DTO）。
 * 说明：
 * - 用于承接前端发送过来的 JSON 注册请求体。
 * - 当前仅用于注册普通用户（role 固定为 USER）。
 */
public class RegisterRequest {

  /** 登录用户名（唯一），对应 t_user.username */
  @NotBlank(message = "用户名不能为空")
  private String username;

  /** 显示昵称，对应 t_user.nickname */
  @NotBlank(message = "昵称不能为空")
  private String nickname;

  /** 登录密码，对应 t_user.password（示例项目中暂为明文存储） */
  @NotBlank(message = "密码不能为空")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

