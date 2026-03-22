package com.campus.backend.model;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库表 t_user。
 * 说明：
 * - 用于承载从数据库查询出来的用户数据。
 * - 字段名使用驼峰命名，依靠 MyBatis 的 map-underscore-to-camel-case 配置
 *   自动与下划线风格的列名（如 last_login_time）进行映射。
 */
public class User {

  /** 主键ID，对应 t_user.id */
  private Long id;

  /** 登录用户名，对应 t_user.username */
  private String username;

  /** 登录密码（明文或加密后字符串），对应 t_user.password */
  private String password;

  /** 昵称，对应 t_user.nickname */
  private String nickname;

  /** 头像地址，对应 t_user.avatar_url */
  private String avatarUrl;

  /** 性别：1男 2女 0未知，对应 t_user.gender */
  private Integer gender;

  /** 年龄，对应 t_user.age */
  private Integer age;

  /** 个人简介，对应 t_user.bio */
  private String bio;

  /** 角色（USER / ADMIN），对应 t_user.role */
  private String role;

  /** 状态（0禁用 1正常），对应 t_user.status */
  private Integer status;

  /** 最后登录时间，对应 t_user.last_login_time */
  private LocalDateTime lastLoginTime;

  /** 创建时间，对应 t_user.create_time */
  private LocalDateTime createTime;

  /** 更新时间，对应 t_user.update_time */
  private LocalDateTime updateTime;

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
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

  public Integer getGender() {
    return gender;
  }

  public void setGender(Integer gender) {
    this.gender = gender;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public LocalDateTime getLastLoginTime() {
    return lastLoginTime;
  }

  public void setLastLoginTime(LocalDateTime lastLoginTime) {
    this.lastLoginTime = lastLoginTime;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }
}

