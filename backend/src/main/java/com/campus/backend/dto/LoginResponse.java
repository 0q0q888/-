package com.campus.backend.dto;

/**
 * 登录成功后的响应数据。
 * 说明：
 * - 用于返回给前端展示的用户基础信息。
 * - 此处未包含 token，只用于最基础的登录演示。
 */
public class LoginResponse {

  /** 用户ID */
  private Long id;

  /** 登录用户名 */
  private String username;

  /** 用户昵称 */
  private String nickname;

  /** 头像地址 */
  private String avatarUrl;

  /** 角色（USER / ADMIN） */
  private String role;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}

