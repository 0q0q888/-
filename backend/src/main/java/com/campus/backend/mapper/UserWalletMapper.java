package com.campus.backend.mapper;

import com.campus.backend.model.UserWallet;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户钱包 Mapper，SQL 定义在 resources/mapper/UserWalletMapper.xml。
 */
@Mapper
public interface UserWalletMapper {

  UserWallet findByUserId(@Param("userId") Long userId);

  int insert(@Param("userId") Long userId);

  int deduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

  int add(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
