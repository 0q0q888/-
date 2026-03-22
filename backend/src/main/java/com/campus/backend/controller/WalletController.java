package com.campus.backend.controller;

import com.campus.backend.service.WalletService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包相关接口：余额查询。
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @GetMapping("/balance")
  public ResponseEntity<Map<String, Object>> getBalance(@RequestParam("userId") Long userId) {
    BigDecimal balance = walletService.getBalance(userId);
    return ResponseEntity.ok(Map.of("balance", balance));
  }
}
