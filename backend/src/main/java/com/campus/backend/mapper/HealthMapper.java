package com.campus.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 健康检查相关的数据库访问接口（Mapper）。
 * 说明：
 * - 仅包含一个非常简单的 SQL：SELECT 1，用来测试数据库连接是否正常。
 */
@Mapper
public interface HealthMapper {

  /**
   * 执行简单的 SELECT 1 语句。
   *
   * @return 通常返回数字 1，表示数据库连接和查询都成功
   */
  @Select("SELECT 1")
  Integer ping();
}

