package com.campus.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地静态资源映射配置。
 *
 * <p>将本地 uploads 目录映射为可访问的 /uploads/** URL。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${app.upload-dir:uploads}")
  private String uploadDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
    String location = root.toUri().toString(); // file:/.../uploads/
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(location)
        .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
  }
}

