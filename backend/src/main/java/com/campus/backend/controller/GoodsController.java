package com.campus.backend.controller;

import com.campus.backend.dto.CreateGoodsOrderRequest;
import com.campus.backend.dto.CreateGoodsRequest;
import com.campus.backend.model.Goods;
import com.campus.backend.model.GoodsOrder;
import com.campus.backend.service.GoodsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 二手商品及订单相关接口。
 * 对应前端 GoodsMarket.vue 中使用的 API：
 * - GET  /api/goods             商品列表（支持关键字搜索）；
 * - POST /api/goods             发布商品；
 * - POST /api/goods/orders      创建订单（立即购买）；
 * - POST /api/goods/chat        “私聊我”：为商品和买家创建会话并发送首条消息；
 * - GET  /api/goods/my-bought   我买到的订单列表；
 * - GET  /api/goods/my-sold     我卖出的订单列表。
 */
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

  private final GoodsService goodsService;

  public GoodsController(GoodsService goodsService) {
    this.goodsService = goodsService;
  }

  /**
   * 商品列表：支持关键字搜索。
   */
  @GetMapping
  public ResponseEntity<List<Goods>> listGoods(
      @RequestParam(value = "keyword", required = false) String keyword) {
    return ResponseEntity.ok(goodsService.listGoods(keyword));
  }

  /**
   * 商品详情。
   * 对应前端：GET /api/goods/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<Goods> getGoods(@PathVariable("id") Long id) {
    return ResponseEntity.ok(goodsService.getGoodsById(id));
  }

  /**
   * 发布商品。
   * 对应前端：POST /api/goods?sellerId=xxx
   */
  @PostMapping
  public ResponseEntity<Goods> createGoods(
      @RequestParam("sellerId") Long sellerId,
      @RequestBody CreateGoodsRequest request) {
    Goods goods = goodsService.createGoods(
        sellerId,
        request.getTitle(),
        request.getDescription(),
        request.getPrice(),
        request.getImages());
    return ResponseEntity.ok(goods);
  }

  /**
   * 创建订单（立即购买）。
   * 对应前端：POST /api/goods/orders，body: { goodsId, buyerId }
   */
  @PostMapping("/orders")
  public ResponseEntity<GoodsOrder> createOrder(
      @RequestBody CreateGoodsOrderRequest request) {
    GoodsOrder order = goodsService.createOrder(request.getGoodsId(), request.getBuyerId());
    return ResponseEntity.ok(order);
  }

  /**
   * “私聊我”：为指定商品和买家创建/复用订单，并发送第一条聊天消息。
   *
   * <p>对应前端：点击商品卡片上的“私聊我”按钮。</p>
   * <p>请求方式：POST /api/goods/chat?goodsId=1&buyerId=2</p>
   *
   * @param goodsId 商品ID
   * @param buyerId 买家用户ID（当前登录用户）
   */
  @PostMapping("/chat")
  public ResponseEntity<Void> startChat(
      @RequestParam("goodsId") Long goodsId,
      @RequestParam("buyerId") Long buyerId) {
    goodsService.startChat(goodsId, buyerId);
    return ResponseEntity.ok().build();
  }

  /**
   * 我买到的订单列表。
   */
  @GetMapping("/my-bought")
  public ResponseEntity<List<GoodsOrder>> myBought(@RequestParam("userId") Long userId) {
    return ResponseEntity.ok(goodsService.listMyBought(userId));
  }

  /**
   * 我卖出的订单列表。
   */
  @GetMapping("/my-sold")
  public ResponseEntity<List<GoodsOrder>> mySold(@RequestParam("userId") Long userId) {
    return ResponseEntity.ok(goodsService.listMySold(userId));
  }

  /**
   * 卖家确认收货：钱款转入卖家钱包，订单状态变为已完成。
   * 对应前端：我卖出的 -> 点击「确认收货」
   */
  @PostMapping("/orders/confirm-receipt")
  public ResponseEntity<Void> confirmReceipt(
      @RequestParam("orderId") Long orderId,
      @RequestParam("sellerId") Long sellerId) {
    goodsService.confirmReceipt(orderId, sellerId);
    return ResponseEntity.ok().build();
  }

  /**
   * 统一处理与二手商品流程相关的业务异常，将其转换为 HTTP 400。
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}

