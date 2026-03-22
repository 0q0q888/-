package com.campus.backend.mapper;

import com.campus.backend.model.Task;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务 Mapper，SQL 定义在 resources/mapper/TaskMapper.xml，与 campus_service.sql 表结构一致。
 */
@Mapper
public interface TaskMapper {

  List<Task> findAll();

  Task findById(@Param("id") Long id);

  List<Task> findMyAccepted(@Param("userId") Long userId);

  List<Task> findMyPublished(@Param("userId") Long userId);

  int takeTask(@Param("taskId") Long taskId, @Param("receiverId") Long receiverId);

  int finishTask(@Param("taskId") Long taskId);

  int cancelTask(@Param("taskId") Long taskId);

  int insert(Task task);
}
