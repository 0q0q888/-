package com.campus.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传接口：用于头像等本地资源上传。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

  @Value("${app.upload-dir:uploads}")
  private String uploadDir;

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("请选择图片文件");
    }
    if (file.getSize() > 3 * 1024 * 1024) {
      throw new IllegalArgumentException("图片大小不能超过 3MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("仅支持图片文件");
    }

    String original = file.getOriginalFilename();
    String ext = StringUtils.getFilenameExtension(original);
    if (ext == null || ext.isBlank()) {
      ext = contentType.replace("image/", ""); // png/jpeg/webp...
    }
    ext = ext.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (ext.isBlank()) {
      ext = "png";
    }

    Path avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
    Files.createDirectories(avatarDir);

    String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
    Path target = avatarDir.resolve(filename);
    Files.copy(file.getInputStream(), target);

    String url = "/uploads/avatars/" + filename;
    return ResponseEntity.ok(Map.of("url", url));
  }

  /**
   * 上传商品图片（用于二手交易“卖出宝贝”本地照片上传）。
   * 说明：
   * - 和头像上传逻辑类似，只是保存目录改为 uploads/goods；
   * - 同样只允许图片类型，默认最大大小限制为 5MB。
   */
  @PostMapping(value = "/goods", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> uploadGoodsImage(
      @RequestParam("file") MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("请选择图片文件");
    }
    if (file.getSize() > 5 * 1024 * 1024) {
      throw new IllegalArgumentException("图片大小不能超过 5MB");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("仅支持图片文件");
    }

    String original = file.getOriginalFilename();
    String ext = StringUtils.getFilenameExtension(original);
    if (ext == null || ext.isBlank()) {
      ext = contentType.replace("image/", "");
    }
    ext = ext.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (ext.isBlank()) {
      ext = "png";
    }

    Path goodsDir = Paths.get(uploadDir, "goods").toAbsolutePath().normalize();
    Files.createDirectories(goodsDir);

    String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
    Path target = goodsDir.resolve(filename);
    Files.copy(file.getInputStream(), target);

    String url = "/uploads/goods/" + filename;
    return ResponseEntity.ok(Map.of("url", url));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArg(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}

