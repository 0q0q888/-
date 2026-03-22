package com.campus.backend.mapper;

import com.campus.backend.model.GoodsOrder;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 二手交易订单 Mapper，SQL 定义在 resources/mapper/GoodsOrderMapper.xml。
 * 说明：
 * - 一个订单对应某个买家和某个商品之间的一次交易/意向（用于聊天和后续确认收货）；
 * - 聊天会话以订单 ID 为锚点（t_goods_message.order_id 外键指向本表）。
 */
@Mapper
public interface GoodsOrderMapper {

  /** 新增订单。 */
  int insert(GoodsOrder order);

  /** 我买到的订单列表：buyer_id = 当前用户。 */
  List<GoodsOrder> findMyBought(@Param("buyerId") Long buyerId);

  /** 我卖出的订单列表：seller_id = 当前用户。 */
  List<GoodsOrder> findMySold(@Param("sellerId") Long sellerId);

  /** 根据订单ID查询。 */
  GoodsOrder findById(@Param("id") Long id);

  /** 更新订单状态。 */
  int updateStatus(@Param("id") Long id, @Param("status") String status);

  /**
   * 根据商品ID和买家ID查询一条订单（若存在则返回第一条）。
   * 说明：
   * - 用于“私聊我”功能，尽量复用已有的订单 ID 作为会话锚点，避免重复创建太多记录；
   * - 若不存在则由业务层决定是否新建订单。
   */
  GoodsOrder findByGoodsAndBuyer(@Param("goodsId") Long goodsId, @Param("buyerId") Long buyerId);
}

