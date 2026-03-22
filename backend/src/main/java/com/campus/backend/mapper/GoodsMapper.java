package com.campus.backend.mapper;

import com.campus.backend.model.Goods;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 二手商品 Mapper，SQL 定义在 resources/mapper/GoodsMapper.xml。
 */
@Mapper
public interface GoodsMapper {

  /**
   * 按关键字查询商品列表。
   * 说明：
   * - keyword 允许为 null 或空字符串，此时返回全部在售/已售商品；
   * - 关键字匹配 title 或 description（模糊匹配）。
   */
  List<Goods> findList(@Param("keyword") String keyword);

  /** 根据ID查询单个商品。 */
  Goods findById(@Param("id") Long id);

  /** 新增商品。 */
  int insert(Goods goods);

  /** 更新商品状态（如从 ON_SALE 置为 SOLD 或 OFF_SHELF）。 */
  int updateStatus(@Param("id") Long id, @Param("status") String status);
}

