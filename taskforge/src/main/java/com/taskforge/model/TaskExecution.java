package com.taskforge.model;

import java.time.Instant;

public class TaskExecution {

    private String id;
    private String workflowExecutionId;
    private String taskId;
    private TaskStatus status;
    private int attempt;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    public TaskExecution() {
    }

    public TaskExecution(String id, String workflowExecutionId, String taskId, TaskStatus status, int attempt,
                         String errorMessage, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.workflowExecutionId = workflowExecutionId;
        this.taskId = taskId;
        this.status = status;
        this.attempt = attempt;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
