package com.campus.backend.service;

import com.campus.backend.dto.SendMessageRequest;
import com.campus.backend.mapper.GoodsMapper;
import com.campus.backend.mapper.GoodsOrderMapper;
import com.campus.backend.model.Goods;
import com.campus.backend.model.GoodsOrder;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 二手商品及订单相关业务逻辑。
 * 说明：
 * - 商品列表、发布商品；
 * - 创建订单（暂不做钱包金额流转，后续可结合 WalletService 扩展）；
 * - 基于订单 ID 的二手商品聊天（“私聊我”）的会话创建。
 */
@Service
public class GoodsService {

  private final GoodsMapper goodsMapper;
  private final GoodsOrderMapper goodsOrderMapper;
  private final MessageService messageService;
  private final WalletService walletService;

  public GoodsService(GoodsMapper goodsMapper, GoodsOrderMapper goodsOrderMapper,
      MessageService messageService, WalletService walletService) {
    this.goodsMapper = goodsMapper;
    this.goodsOrderMapper = goodsOrderMapper;
    this.messageService = messageService;
    this.walletService = walletService;
  }

  /**
   * 按关键字查询商品列表。
   *
   * @param keyword 关键字，可为 null 或空字符串
   * @return 商品列表
   */
  public List<Goods> listGoods(String keyword) {
    String kw = keyword == null ? null : keyword.trim();
    if (kw != null && kw.isEmpty()) {
      kw = null;
    }
    return goodsMapper.findList(kw);
  }

  /**
   * 根据商品ID查询详情。
   *
   * @param id 商品ID
   * @return 对应的商品实体
   */
  public Goods getGoodsById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("商品ID不能为空");
    }
    Goods goods = goodsMapper.findById(id);
    if (goods == null) {
      throw new IllegalArgumentException("商品不存在");
    }
    return goods;
  }

  /**
   * 发布商品。
   */
  public Goods createGoods(Long sellerId, String title, String description, Double price,
      String images) {
    if (sellerId == null) {
      throw new IllegalArgumentException("发布人不能为空");
    }
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("商品标题不能为空");
    }
    if (images == null || images.isBlank()) {
      throw new IllegalArgumentException("请至少上传一张商品图片");
    }
    if (price != null && price < 0) {
      throw new IllegalArgumentException("价格不能为负数");
    }
    Goods goods = new Goods();
    goods.setSellerId(sellerId);
    goods.setTitle(title.trim());
    goods.setDescription(description == null ? "" : description.trim());
    goods.setPrice(price);
    goods.setImages(images.trim());
    goods.setStatus("ON_SALE");
    goodsMapper.insert(goods);
    return goodsMapper.findById(goods.getId());
  }

  /**
   * 创建订单：买家对某个商品下单。
   * 说明：
   * - 当前实现仅负责创建订单记录和将商品标记为 SOLD，暂不做钱包金额扣减/结算；
   * - 后续可以在此处接入 WalletService 实现“确认收货后完成欠款转移”。
   */
  @Transactional
  public GoodsOrder createOrder(Long goodsId, Long buyerId) {
    if (goodsId == null) {
      throw new IllegalArgumentException("商品ID不能为空");
    }
    if (buyerId == null) {
      throw new IllegalArgumentException("买家ID不能为空");
    }
    Goods goods = goodsMapper.findById(goodsId);
    if (goods == null) {
      throw new IllegalArgumentException("商品不存在");
    }
    if (!"ON_SALE".equals(goods.getStatus())) {
      throw new IllegalArgumentException("商品当前不可购买");
    }
    if (goods.getSellerId() != null && goods.getSellerId().equals(buyerId)) {
      throw new IllegalArgumentException("不能购买自己发布的商品");
    }
    if (goods.getPrice() == null) {
      throw new IllegalArgumentException("该商品暂无报价，暂不支持购买，请私聊卖家议价");
    }

    double priceVal = goods.getPrice();
    BigDecimal amount = BigDecimal.valueOf(priceVal);

    GoodsOrder order = new GoodsOrder();
    order.setGoodsId(goodsId);
    order.setBuyerId(buyerId);
    order.setSellerId(goods.getSellerId());
    order.setPrice(priceVal);
    order.setStatus("CREATED");
    goodsOrderMapper.insert(order);

    if (!walletService.deductForGoods(buyerId, amount, order.getId(), "购买：" + goods.getTitle())) {
      throw new IllegalArgumentException("余额不足，请先充值");
    }

    goodsOrderMapper.updateStatus(order.getId(), "PAID");
    order.setStatus("PAID");
    goodsMapper.updateStatus(goodsId, "SOLD");

    order.setGoodsTitle(goods.getTitle());
    return order;
  }

  /**
   * 为“私聊我”功能创建或复用订单记录。
   *
   * <p>说明：</p>
   * <ul>
   *   <li>每个买家在同一件商品上，尽量复用一条订单记录作为聊天会话的锚点（t_goods_message.order_id）；</li>
   *   <li>若不存在对应订单，则新建一条状态为 CREATED 的订单，不会修改商品的售卖状态；</li>
   *   <li>该方法不会触发任何钱包扣款，仅用于绑定聊天上下文。</li>
   * </ul>
   *
   * @param goodsId 商品ID
   * @param buyerId 买家用户ID
   * @return 复用或新建的订单实体（带 goodsTitle）
   */
  @Transactional
  public GoodsOrder createChatOrderIfAbsent(Long goodsId, Long buyerId) {
    if (goodsId == null) {
      throw new IllegalArgumentException("商品ID不能为空");
    }
    if (buyerId == null) {
      throw new IllegalArgumentException("买家ID不能为空");
    }
    Goods goods = goodsMapper.findById(goodsId);
    if (goods == null) {
      throw new IllegalArgumentException("商品不存在");
    }
    if (!"ON_SALE".equals(goods.getStatus())) {
      throw new IllegalArgumentException("商品当前不可聊天");
    }
    if (goods.getSellerId() != null && goods.getSellerId().equals(buyerId)) {
      throw new IllegalArgumentException("不能和自己就该商品发起私聊");
    }

    GoodsOrder existing = goodsOrderMapper.findByGoodsAndBuyer(goodsId, buyerId);
    if (existing != null) {
      // 补齐商品标题，便于前端展示
      if (existing.getGoodsTitle() == null) {
        existing.setGoodsTitle(goods.getTitle());
      }
      return existing;
    }

    GoodsOrder order = new GoodsOrder();
    order.setGoodsId(goodsId);
    order.setBuyerId(buyerId);
    order.setSellerId(goods.getSellerId());
    order.setPrice(goods.getPrice() != null ? goods.getPrice() : 0.0);
    order.setStatus("CREATED");
    goodsOrderMapper.insert(order);
    order.setGoodsTitle(goods.getTitle());
    return order;
  }

  /**
   * “私聊我”：为指定商品和买家创建/复用订单，并自动发送第一条聊天消息。
   *
   * <p>对应前端点击“私聊我”的动作：</p>
   * <ol>
   *   <li>为 goodsId + buyerId 创建/查找一条 CREATED 状态的订单；</li>
   *   <li>构造一条简短的问候消息（「你好，我对 XXX 感兴趣～」）；</li>
   *   <li>调用 MessageService 将消息落库并通过 WebSocket 推送给卖家。</li>
   * </ol>
   *
   * @param goodsId 商品ID
   * @param buyerId 买家用户ID（当前登录用户）
   */
  @Transactional
  public void startChat(Long goodsId, Long buyerId) {
    Goods goods = goodsMapper.findById(goodsId);
    if (goods == null) {
      throw new IllegalArgumentException("商品不存在");
    }
    GoodsOrder order = createChatOrderIfAbsent(goodsId, buyerId);
    SendMessageRequest req = new SendMessageRequest();
    req.setType("GOODS");
    req.setBizId(order.getId()); // 对应 t_goods_message.order_id
    req.setSenderId(buyerId);
    req.setReceiverId(order.getSellerId());
    String title = goods.getTitle() == null ? "" : goods.getTitle().trim();
    String content =
        "你好，我对「" + (title.isEmpty() ? String.valueOf(goodsId) : title) + "」感兴趣～";
    req.setContent(content);
    messageService.send(req);
  }

  /**
   * 我买到的订单列表。
   */
  public List<GoodsOrder> listMyBought(Long buyerId) {
    if (buyerId == null) {
      throw new IllegalArgumentException("用户ID不能为空");
    }
    return goodsOrderMapper.findMyBought(buyerId);
  }

  /**
   * 卖家确认收货：钱款从买家（已扣）转入卖家钱包，订单状态变为 FINISHED。
   */
  @Transactional
  public void confirmReceipt(Long orderId, Long sellerId) {
    if (orderId == null) {
      throw new IllegalArgumentException("订单ID不能为空");
    }
    if (sellerId == null) {
      throw new IllegalArgumentException("卖家ID不能为空");
    }
    GoodsOrder order = goodsOrderMapper.findById(orderId);
    if (order == null) {
      throw new IllegalArgumentException("订单不存在");
    }
    if (!sellerId.equals(order.getSellerId())) {
      throw new IllegalArgumentException("只有卖家本人可确认收货");
    }
    if (!"PAID".equals(order.getStatus())) {
      throw new IllegalArgumentException("该订单当前不支持确认收货");
    }

    BigDecimal amount = BigDecimal.valueOf(order.getPrice());
    walletService.addForGoods(sellerId, amount, orderId, "确认收货：" + order.getGoodsTitle());
    goodsOrderMapper.updateStatus(orderId, "FINISHED");
  }

  /**
   * 我卖出的订单列表。
   */
  public List<GoodsOrder> listMySold(Long sellerId) {
    if (sellerId == null) {
      throw new IllegalArgumentException("用户ID不能为空");
    }
    return goodsOrderMapper.findMySold(sellerId);
  }
}

