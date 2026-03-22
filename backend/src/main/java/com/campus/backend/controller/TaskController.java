package com.campus.backend.controller;

import com.campus.backend.dto.PublishTaskRequest;
import com.campus.backend.model.Task;
import com.campus.backend.service.TaskService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务相关的控制器，负责任务大厅的接口。
 * 说明：
 * - /api/tasks：任务列表（任务大厅）；
 * - /api/tasks/{id}：任务详情；
 * - /api/tasks/{id}/take：接单。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  /** 任务业务逻辑服务对象，用于处理任务相关操作 */
  private final TaskService taskService;

  /**
   * 构造方法，用于注入 TaskService。
   *
   * @param taskService 任务相关业务逻辑服务
   */
  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  /**
   * 发布任务。
   * POST /api/tasks?publisherId=xxx，请求体：title, description, rewardAmount, deadlineTime。
   */
  @PostMapping
  public ResponseEntity<Task> createTask(
      @RequestParam("publisherId") Long publisherId,
      @RequestBody PublishTaskRequest request) {
    Task task = taskService.createTask(
        publisherId,
        request.getTitle(),
        request.getDescription(),
        request.getRewardAmount(),
        request.getDeadlineTime());
    return ResponseEntity.ok(taskService.getTaskById(task.getId()));
  }

  @GetMapping
  public ResponseEntity<List<Task>> listTasks() {
    List<Task> tasks = taskService.listTasks();
    return ResponseEntity.ok(tasks);
  }

  /**
   * 任务详情接口：根据任务ID查询详情。
   *
   * 请求方式：GET
   * 请求路径：/api/tasks/{id}
   *
   * @param id 路径中的任务ID
   * @return 返回对应任务的详细信息
   */
  @GetMapping("/{id}")
  public ResponseEntity<Task> getTask(@PathVariable("id") Long id) {
    Task task = taskService.getTaskById(id);
    return ResponseEntity.ok(task);
  }

  /**
   * 接单接口：当前用户为指定任务接单。
   *
   * 请求方式：POST
   * 请求路径：/api/tasks/{id}/take
   * 请求参数：receiverId（接单人用户ID，暂时从前端传入）
   *
   * @param id         路径中的任务ID
   * @param receiverId 接单人用户ID
   * @return 200 OK 表示接单成功
   */
  @PostMapping("/{id}/take")
  public ResponseEntity<Void> takeTask(
      @PathVariable("id") Long id, @RequestParam("receiverId") Long receiverId) {
    taskService.takeTask(id, receiverId);
    return ResponseEntity.ok().build();
  }

  /**
   * 我接的单：receiver_id = 当前用户。
   */
  @GetMapping("/my-accepted")
  public ResponseEntity<List<Task>> myAccepted(@RequestParam("userId") Long userId) {
    return ResponseEntity.ok(taskService.listMyAccepted(userId));
  }

  /**
   * 我发布的订单：publisher_id = 当前用户（会先处理超时退回再返回）。
   */
  @GetMapping("/my-published")
  public ResponseEntity<List<Task>> myPublished(@RequestParam("userId") Long userId) {
    return ResponseEntity.ok(taskService.listMyPublished(userId));
  }

  /**
   * 任务完成：发布人确认，将酬劳转给接单人。
   */
  @PostMapping("/{id}/complete")
  public ResponseEntity<Void> completeTask(
      @PathVariable("id") Long id, @RequestParam("publisherId") Long publisherId) {
    taskService.completeTask(id, publisherId);
    return ResponseEntity.ok().build();
  }

  /**
   * 处理任务相关过程中抛出的 IllegalArgumentException 异常。
   * 说明：
   * - 用于将业务异常转换为 HTTP 400 响应，并在响应体中返回错误信息。
   *
   * @param ex 任务流程中抛出的非法参数异常
   * @return 包含错误信息的 400 Bad Request 响应
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
}

