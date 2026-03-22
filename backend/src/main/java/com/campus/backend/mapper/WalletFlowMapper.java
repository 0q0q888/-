package com.campus.backend.mapper;

import com.campus.backend.model.WalletFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 钱包流水 Mapper，SQL 定义在 resources/mapper/WalletFlowMapper.xml。
 */
@Mapper
public interface WalletFlowMapper {

  int insert(WalletFlow flow);
}
