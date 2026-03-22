package com.campus.backend.service;

import com.campus.backend.dto.SendMessageRequest;
import com.campus.backend.mapper.TaskMapper;
import com.campus.backend.model.Task;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务相关业务逻辑服务类。
 * 说明：
 * - 任务列表、详情、接单、完成、超时退回。
 */
@Service
public class TaskService {

  private final TaskMapper taskMapper;
  private final WalletService walletService;
  private final MessageService messageService;

  public TaskService(TaskMapper taskMapper, WalletService walletService,
      MessageService messageService) {
    this.taskMapper = taskMapper;
    this.walletService = walletService;
    this.messageService = messageService;
  }

  /**
   * 查询所有任务列表。
   * 说明：
   * - 当前简单返回全部任务，按创建时间倒序；
   * - 后续可以在这里根据关键字、状态、酬劳区间等增加过滤逻辑。
   *
   * @return 任务列表
   */
  public List<Task> listTasks() {
    return taskMapper.findAll();
  }

  /**
   * 发布任务。
   */
  public Task createTask(Long publisherId, String title, String description,
      Double rewardAmount, java.time.LocalDateTime deadlineTime) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("任务标题不能为空");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("任务描述不能为空");
    }
    if (rewardAmount == null || rewardAmount < 0) {
      throw new IllegalArgumentException("酬劳不能为负");
    }
    Task task = new Task();
    task.setTitle(title.trim());
    task.setDescription(description.trim());
    task.setRewardAmount(rewardAmount);
    task.setPublisherId(publisherId);
    task.setStatus("OPEN");
    task.setDeadlineTime(deadlineTime);
    taskMapper.insert(task);
    return task;
  }

  /**
   * 根据任务ID查询任务详情。
   *
   * @param id 任务ID
   * @return 对应的 Task 实体
   * @throws IllegalArgumentException 当任务不存在时抛出
   */
  public Task getTaskById(Long id) {
    Task task = taskMapper.findById(id);
    if (task == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    return task;
  }

  /**
   * 用户接单操作。校验截止时间、发布人余额后扣款并接单。
   */
  @Transactional
  public void takeTask(Long taskId, Long receiverId) {
    Task task = taskMapper.findById(taskId);
    if (task == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    if (!"OPEN".equals(task.getStatus())) {
      throw new IllegalArgumentException("任务当前不可接单");
    }
    if (task.getPublisherId() != null && task.getPublisherId().equals(receiverId)) {
      throw new IllegalArgumentException("不能接自己发布的任务");
    }
    LocalDateTime now = LocalDateTime.now();
    if (task.getDeadlineTime() != null && now.isAfter(task.getDeadlineTime())) {
      throw new IllegalArgumentException("已过接单截止时间，无法接单");
    }
    BigDecimal reward = toBigDecimal(task.getRewardAmount());
    if (reward.compareTo(BigDecimal.ZERO) > 0) {
      boolean deducted = walletService.deduct(
          task.getPublisherId(), reward, taskId, "任务接单锁定");
      if (!deducted) {
        throw new IllegalArgumentException("发布人余额不足，无法接单");
      }
    }
    int updated = taskMapper.takeTask(taskId, receiverId);
    if (updated == 0) {
      throw new IllegalArgumentException("接单失败，请稍后重试");
    }

    // 接单成功后自动创建一条任务会话消息，方便双方直接在“我的消息”里聊天
    try {
      SendMessageRequest req = new SendMessageRequest();
      req.setType("TASK");
      req.setBizId(taskId);
      req.setSenderId(receiverId);
      req.setReceiverId(task.getPublisherId());
      String title = task.getTitle() == null ? "" : task.getTitle().trim();
      String content = "我已接下任务「" + (title.isEmpty() ? String.valueOf(taskId) : title) + "」，可以在这里沟通细节～";
      req.setContent(content);
      messageService.send(req);
    } catch (Exception e) {
      // 自动创建对话失败不影响接单本身，忽略异常
    }
  }

  /** 我接的单（receiver_id = 当前用户） */
  public List<Task> listMyAccepted(Long userId) {
    return taskMapper.findMyAccepted(userId);
  }

  /** 我发布的订单（publisher_id = 当前用户）。先处理超时退回，再返回列表。 */
  @Transactional
  public List<Task> listMyPublished(Long userId) {
    processExpiredForUser(userId);
    return taskMapper.findMyPublished(userId);
  }

  /**
   * 任务完成：发布人确认，将酬劳转给接单人。
   */
  @Transactional
  public void completeTask(Long taskId, Long publisherId) {
    Task task = taskMapper.findById(taskId);
    if (task == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    if (!task.getPublisherId().equals(publisherId)) {
      throw new IllegalArgumentException("仅发布人可确认完成");
    }
    if (!"TAKEN".equals(task.getStatus())) {
      throw new IllegalArgumentException("当前任务状态不可确认完成");
    }
    BigDecimal reward = toBigDecimal(task.getRewardAmount());
    if (reward.compareTo(BigDecimal.ZERO) > 0 && task.getReceiverId() != null) {
      walletService.add(task.getReceiverId(), reward, taskId, "任务完成酬劳");
    }
    int updated = taskMapper.finishTask(taskId);
    if (updated == 0) {
      throw new IllegalArgumentException("确认完成失败，请稍后重试");
    }
  }

  /**
   * 将当前用户已发布且已超时的 TAKEN 任务做退回处理（金额退回发布人，状态改为 CANCELLED）。
   */
  private void processExpiredForUser(Long publisherId) {
    List<Task> myPublished = taskMapper.findMyPublished(publisherId);
    LocalDateTime now = LocalDateTime.now();
    List<Task> expired = myPublished.stream()
        .filter(t -> "TAKEN".equals(t.getStatus()))
        .filter(t -> t.getDeadlineTime() != null && now.isAfter(t.getDeadlineTime()))
        .collect(Collectors.toList());
    for (Task t : expired) {
      BigDecimal reward = toBigDecimal(t.getRewardAmount());
      if (reward.compareTo(BigDecimal.ZERO) > 0) {
        walletService.add(t.getPublisherId(), reward, t.getId(), "任务超时退回");
      }
      taskMapper.cancelTask(t.getId());
    }
  }

  private static BigDecimal toBigDecimal(Double d) {
    if (d == null) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(d);
  }
}

