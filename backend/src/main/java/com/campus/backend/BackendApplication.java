package com.campus.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口类。
 * 说明：
 * - @SpringBootApplication：开启 Spring Boot 自动配置与组件扫描。
 * - @MapperScan：指定 MyBatis 的 Mapper 接口所在包，自动生成实现类并注入到 Spring 容器。
 */
@SpringBootApplication
@MapperScan("com.campus.backend.mapper")
public class BackendApplication {

  /**
   * main 方法：应用启动入口。
   *
   * @param args 命令行参数（当前项目中未使用）
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }
}

