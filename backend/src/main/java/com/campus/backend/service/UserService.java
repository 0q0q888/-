package com.campus.backend.service;

import com.campus.backend.dto.UpdateProfileRequest;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.model.User;
import org.springframework.stereotype.Service;

/**
 * 用户相关业务逻辑。
 */
@Service
public class UserService {

  private final UserMapper userMapper;

  public UserService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /**
   * 获取用户资料（不含密码）。
   */
  public User getProfile(Long id) {
    User user = userMapper.findByIdWithoutPassword(id);
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    return user;
  }

  /**
   * 更新个人资料。
   */
  public void updateProfile(Long id, UpdateProfileRequest request) {
    User existing = userMapper.findById(id);
    if (existing == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    String nickname = request.getNickname() != null ? request.getNickname() : existing.getNickname();
    String avatarUrl = request.getAvatarUrl() != null ? request.getAvatarUrl() : existing.getAvatarUrl();
    Integer gender = request.getGender() != null ? request.getGender() : existing.getGender();
    Integer age = request.getAge() != null ? request.getAge() : existing.getAge();
    int updated = userMapper.updateProfile(id, nickname, avatarUrl, gender, age);
    if (updated == 0) {
      throw new IllegalArgumentException("更新失败");
    }
  }
}
