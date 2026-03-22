package com.campus.backend.service;

import com.campus.backend.dto.ChangePasswordRequest;
import com.campus.backend.dto.LoginRequest;
import com.campus.backend.dto.LoginResponse;
import com.campus.backend.dto.RegisterRequest;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.model.User;
import org.springframework.stereotype.Service;

/**
 * 认证相关业务逻辑服务类。
 * 说明：
 * - 目前包含“登录”和“注册普通用户”两个基础功能。
 * - 后续可以在此扩展修改密码、退出登录等逻辑。
 */
@Service
public class AuthService {

  /** 用户表的数据库访问对象，用于查询用户信息 */
  private final UserMapper userMapper;

  /**
   * 构造方法，用于注入 UserMapper。
   *
   * @param userMapper 用户相关的 Mapper，用于访问 t_user 表
   */
  public AuthService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /**
   * 处理用户登录逻辑。
   *
   * 步骤：
   * 1. 根据用户名从数据库查询用户；
   * 2. 校验用户是否存在、是否被禁用；
   * 3. 校验密码是否匹配（此处为演示，直接明文比较）；
   * 4. 登录成功后，封装 LoginResponse 返回给 Controller。
   *
   * @param request 登录请求参数，包含用户名和密码
   * @return 登录成功后的响应数据（不含敏感信息）
   * @throws IllegalArgumentException 当用户名不存在、用户被禁用或密码错误时抛出
   */
  public LoginResponse login(LoginRequest request) {
    // 1. 根据用户名查询用户
    User user = userMapper.findByUsername(request.getUsername());
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }

    // 2. 检查用户状态（0 表示禁用）
    if (user.getStatus() != null && user.getStatus() == 0) {
      throw new IllegalArgumentException("用户已被禁用");
    }

    // 3. 校验密码（演示项目中为明文比较，真实项目请使用加密存储和匹配）
    if (!user.getPassword().equals(request.getPassword())) {
      throw new IllegalArgumentException("用户名或密码错误");
    }

    // 4. 构造登录成功的响应对象
    LoginResponse response = new LoginResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setNickname(user.getNickname());
    response.setAvatarUrl(user.getAvatarUrl());
    response.setRole(user.getRole());
    return response;
  }

  /**
   * 处理普通用户注册逻辑。
   *
   * 步骤：
   * 1. 根据用户名检查是否已存在（避免重复注册同一用户名）；
   * 2. 构造 User 实体，角色固定为 USER，状态默认为正常（1）；
   * 3. 调用 Mapper 执行插入操作；
   * 4. 构造 LoginResponse 返回给调用方（可用于后续直接登录或展示）。
   *
   * @param request 注册请求参数，包含用户名、昵称和密码
   * @return 注册成功后的用户基础信息
   * @throws IllegalArgumentException 当用户名已存在时抛出
   */
  public LoginResponse register(RegisterRequest request) {
    // 1. 用户名唯一性检查
    User existing = userMapper.findByUsername(request.getUsername());
    if (existing != null) {
      throw new IllegalArgumentException("用户名已存在，请更换一个");
    }

    // 2. 构造待插入的用户实体（此处为演示，密码暂时为明文存储）
    User user = new User();
    user.setUsername(request.getUsername());
    user.setNickname(request.getNickname());
    user.setPassword(request.getPassword());
    // 性别、年龄、简介先留空，后续可在个人中心完善
    user.setGender(null);
    user.setAge(null);
    user.setBio(null);
    user.setRole("USER");
    user.setStatus(1);

    // 3. 插入数据库，user.id 将被自动回填
    userMapper.insert(user);

    // 4. 构造返回对象
    LoginResponse response = new LoginResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setNickname(user.getNickname());
    response.setAvatarUrl(user.getAvatarUrl());
    response.setRole(user.getRole());
    return response;
  }

  /**
   * 修改密码。
   */
  public void changePassword(Long userId, ChangePasswordRequest request) {
    if (request == null || request.getOldPassword() == null || request.getNewPassword() == null) {
      throw new IllegalArgumentException("原密码和新密码不能为空");
    }
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    if (!user.getPassword().equals(request.getOldPassword())) {
      throw new IllegalArgumentException("原密码错误");
    }
    String newPwd = request.getNewPassword().trim();
    if (newPwd.length() < 6) {
      throw new IllegalArgumentException("新密码至少6位");
    }
    int updated = userMapper.updatePassword(userId, newPwd);
    if (updated == 0) {
      throw new IllegalArgumentException("修改失败");
    }
  }
}

