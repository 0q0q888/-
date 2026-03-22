package com.campus.backend.mapper;

import com.campus.backend.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户相关的数据库访问接口（Mapper）。
 */
@Mapper
public interface UserMapper {

  @Select("SELECT id, username, password, nickname, avatar_url, gender, age, bio, role, status, " +
          "last_login_time, create_time, update_time " +
          "FROM t_user WHERE username = #{username} LIMIT 1")
  User findByUsername(@Param("username") String username);

  @Select("SELECT id, username, password, nickname, avatar_url, gender, age, bio, role, status, " +
          "last_login_time, create_time, update_time " +
          "FROM t_user WHERE id = #{id} LIMIT 1")
  User findById(@Param("id") Long id);

  @Select("SELECT id, username, nickname, avatar_url, gender, age, bio, role, status, " +
          "last_login_time, create_time, update_time " +
          "FROM t_user WHERE id = #{id} LIMIT 1")
  User findByIdWithoutPassword(@Param("id") Long id);

  @Update("UPDATE t_user SET nickname = #{nickname}, avatar_url = #{avatarUrl}, gender = #{gender}, age = #{age}, update_time = NOW() WHERE id = #{id}")
  int updateProfile(@Param("id") Long id, @Param("nickname") String nickname, @Param("avatarUrl") String avatarUrl,
                   @Param("gender") Integer gender, @Param("age") Integer age);

  @Update("UPDATE t_user SET password = #{newPassword}, update_time = NOW() WHERE id = #{id}")
  int updatePassword(@Param("id") Long id, @Param("newPassword") String newPassword);

  @Insert("INSERT INTO t_user " +
          "(username, password, nickname, avatar_url, gender, age, bio, role, status, last_login_time, create_time, update_time) " +
          "VALUES " +
          "(#{username}, #{password}, #{nickname}, #{avatarUrl}, #{gender}, #{age}, #{bio}, #{role}, #{status}, #{lastLoginTime}, NOW(), NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(User user);
}

