package com.taskforge.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_execution")
public class TaskExecutionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "workflow_execution_id", nullable = false)
    private String workflowExecutionId;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "outputs", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private java.util.Map<String, Object> outputs = new java.util.HashMap<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_execution_id", insertable = false, updatable = false)
    private WorkflowExecutionEntity workflowExecution;

    public TaskExecutionEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowExecutionId() { return workflowExecutionId; }
    public void setWorkflowExecutionId(String workflowExecutionId) { this.workflowExecutionId = workflowExecutionId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public java.util.Map<String, Object> getOutputs() { return outputs; }
    public void setOutputs(java.util.Map<String, Object> outputs) { this.outputs = outputs; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public WorkflowExecutionEntity getWorkflowExecution() { return workflowExecution; }
    public void setWorkflowExecution(WorkflowExecutionEntity workflowExecution) {
        this.workflowExecution = workflowExecution;
    }
}
