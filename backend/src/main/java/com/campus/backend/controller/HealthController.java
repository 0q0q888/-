package com.campus.backend.controller;

import com.campus.backend.mapper.HealthMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器。
 * 说明：
 * - 提供最基础的接口，用来检查后端应用和数据库是否正常工作。
 * - 在开发阶段可通过浏览器或 Postman 访问这些接口做连通性测试。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

  /** 用于执行简单 SQL（SELECT 1）以检测数据库是否可用的 Mapper 接口 */
  private final HealthMapper healthMapper;

  /**
   * 构造方法，用于注入 HealthMapper。
   *
   * @param healthMapper 健康检查相关的 Mapper，对数据库执行简单查询
   */
  public HealthController(HealthMapper healthMapper) {
    this.healthMapper = healthMapper;
  }

  /**
   * 应用健康检查接口。
   * 请求方式：GET
   * 请求路径：/api/health
   *
   * @return 一个简单的 JSON 对象，例如：{"ok": true}
   */
  @GetMapping
  public Map<String, Object> health() {
    return Map.of("ok", true);
  }

  /**
   * 应用 + 数据库 联合健康检查接口。
   * 请求方式：GET
   * 请求路径：/api/health/db
   *
   * @return JSON 对象，包含应用状态和数据库查询结果，例如：{"ok": true, "db": 1}
   */
  @GetMapping("/db")
  public Map<String, Object> db() {
    Integer ping = healthMapper.ping();
    return Map.of("ok", true, "db", ping);
  }
}

