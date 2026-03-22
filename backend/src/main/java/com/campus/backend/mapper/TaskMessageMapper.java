package com.campus.backend.mapper;

import com.campus.backend.dto.ChatMessageItem;
import com.campus.backend.dto.ConversationItem;
import com.campus.backend.model.TaskMessage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskMessageMapper {

  List<ConversationItem> listTaskConversations(@Param("userId") Long userId);

  List<ChatMessageItem> listTaskHistory(@Param("userId") Long userId,
      @Param("taskId") Long taskId, @Param("otherUserId") Long otherUserId,
      @Param("limit") Integer limit);

  int markTaskRead(@Param("userId") Long userId, @Param("taskId") Long taskId,
      @Param("otherUserId") Long otherUserId);

  int insert(TaskMessage message);

  /** 当前用户在任务消息中的未读总数。 */
  int countTaskUnread(@Param("userId") Long userId);

  /** 删除某个任务会话下当前用户与指定对方之间的所有消息。 */
  int deleteTaskConversation(@Param("userId") Long userId,
      @Param("taskId") Long taskId, @Param("otherUserId") Long otherUserId);
}

