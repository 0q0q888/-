package com.campus.backend.controller;

import com.campus.backend.dto.UpdateProfileRequest;
import com.campus.backend.model.User;
import com.campus.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口：个人资料查询与更新。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<User> getProfile(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userService.getProfile(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<User> updateProfile(
      @PathVariable("id") Long id,
      @RequestBody UpdateProfileRequest request) {
    userService.updateProfile(id, request);
    return ResponseEntity.ok(userService.getProfile(id));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArg(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}
