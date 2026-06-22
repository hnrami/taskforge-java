package com.taskforge.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_execution")
public class WorkflowExecutionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "workflow_definition_id", nullable = false)
    private String workflowDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @OneToMany(mappedBy = "workflowExecution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TaskExecutionEntity> taskExecutions = new ArrayList<>();

    @Column(name = "shared_outputs", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private java.util.Map<String, Object> sharedOutputs = new java.util.HashMap<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    private void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = ExecutionStatus.CREATED;
        }
    }

    public WorkflowExecutionEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowDefinitionId() { return workflowDefinitionId; }
    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public List<TaskExecutionEntity> getTaskExecutions() { return taskExecutions; }
    public void setTaskExecutions(List<TaskExecutionEntity> taskExecutions) {
        this.taskExecutions = taskExecutions;
    }

    public java.util.Map<String, Object> getSharedOutputs() { return sharedOutputs; }
    public void setSharedOutputs(java.util.Map<String, Object> sharedOutputs) {
        this.sharedOutputs = sharedOutputs;
    }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
