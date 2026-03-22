package com.campus.backend.mapper;

import com.campus.backend.dto.ChatMessageItem;
import com.campus.backend.dto.ConversationItem;
import com.campus.backend.model.GoodsMessage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsMessageMapper {

  List<ConversationItem> listGoodsConversations(@Param("userId") Long userId);

  List<ChatMessageItem> listGoodsHistory(@Param("userId") Long userId,
      @Param("orderId") Long orderId, @Param("otherUserId") Long otherUserId,
      @Param("limit") Integer limit);

  int markGoodsRead(@Param("userId") Long userId, @Param("orderId") Long orderId,
      @Param("otherUserId") Long otherUserId);

  int insert(GoodsMessage message);

  /** 当前用户在商品消息中的未读总数。 */
  int countGoodsUnread(@Param("userId") Long userId);

  /** 删除某个订单会话下当前用户与指定对方之间的所有消息。 */
  int deleteGoodsConversation(@Param("userId") Long userId,
      @Param("orderId") Long orderId, @Param("otherUserId") Long otherUserId);
}

