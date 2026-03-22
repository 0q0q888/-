package com.campus.backend.service;

import com.campus.backend.mapper.UserWalletMapper;
import com.campus.backend.mapper.WalletFlowMapper;
import com.campus.backend.model.UserWallet;
import com.campus.backend.model.WalletFlow;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 钱包服务：确保钱包存在、扣款、加款及流水记录。
 */
@Service
public class WalletService {

  private final UserWalletMapper userWalletMapper;
  private final WalletFlowMapper walletFlowMapper;

  public WalletService(UserWalletMapper userWalletMapper, WalletFlowMapper walletFlowMapper) {
    this.userWalletMapper = userWalletMapper;
    this.walletFlowMapper = walletFlowMapper;
  }

  /**
   * 获取用户余额，无钱包则返回 0。
   */
  public java.math.BigDecimal getBalance(Long userId) {
    if (userId == null) {
      return java.math.BigDecimal.ZERO;
    }
    UserWallet wallet = userWalletMapper.findByUserId(userId);
    return wallet != null && wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
  }

  /**
   * 确保用户有钱包记录，没有则创建。
   */
  @Transactional
  public void ensureWallet(Long userId) {
    if (userWalletMapper.findByUserId(userId) == null) {
      userWalletMapper.insert(userId);
    }
  }

  /**
   * 扣减用户余额（如任务接单时锁定发布人金额）。
   *
   * @return true 扣减成功，false 余额不足或用户无钱包
   */
  @Transactional
  public boolean deduct(Long userId, BigDecimal amount, Long taskId, String remark) {
    ensureWallet(userId);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    int updated = userWalletMapper.deduct(userId, amount);
    if (updated == 0) {
      return false;
    }
    WalletFlow flow = new WalletFlow();
    flow.setUserId(userId);
    flow.setAmount(amount.negate());
    flow.setType("EXPENSE");
    flow.setBizType("TASK_REWARD");
    flow.setBizId(taskId);
    flow.setRemark(remark != null ? remark : "任务接单锁定");
    walletFlowMapper.insert(flow);
    return true;
  }

  /**
   * 二手商品交易：扣减买家余额（立即支付）。
   *
   * @return true 扣减成功，false 余额不足
   */
  @Transactional
  public boolean deductForGoods(Long userId, BigDecimal amount, Long orderId, String remark) {
    ensureWallet(userId);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    int updated = userWalletMapper.deduct(userId, amount);
    if (updated == 0) {
      return false;
    }
    WalletFlow flow = new WalletFlow();
    flow.setUserId(userId);
    flow.setAmount(amount.negate());
    flow.setType("EXPENSE");
    flow.setBizType("GOODS_TRADE");
    flow.setBizId(orderId);
    flow.setRemark(remark != null ? remark : "商品购买");
    walletFlowMapper.insert(flow);
    return true;
  }

  /**
   * 二手商品交易：增加卖家余额（确认收货后到账）。
   */
  @Transactional
  public void addForGoods(Long userId, BigDecimal amount, Long orderId, String remark) {
    ensureWallet(userId);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    userWalletMapper.add(userId, amount);
    WalletFlow flow = new WalletFlow();
    flow.setUserId(userId);
    flow.setAmount(amount);
    flow.setType("INCOME");
    flow.setBizType("GOODS_TRADE");
    flow.setBizId(orderId);
    flow.setRemark(remark != null ? remark : "商品售出");
    walletFlowMapper.insert(flow);
  }

  /**
   * 增加用户余额（如任务完成给接单人打款、超时退回给发布人）。
   */
  @Transactional
  public void add(Long userId, BigDecimal amount, Long taskId, String remark) {
    ensureWallet(userId);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    userWalletMapper.add(userId, amount);
    WalletFlow flow = new WalletFlow();
    flow.setUserId(userId);
    flow.setAmount(amount);
    flow.setType("INCOME");
    flow.setBizType("TASK_REWARD");
    flow.setBizId(taskId);
    flow.setRemark(remark != null ? remark : "任务酬劳");
    walletFlowMapper.insert(flow);
  }
}
