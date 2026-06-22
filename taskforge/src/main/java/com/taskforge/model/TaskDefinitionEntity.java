package com.taskforge.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_definition")
public class TaskDefinitionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;          // UUID — unique across all workflows

    @Column(name = "task_id", nullable = false)
    private String taskId;      // logical task name e.g. "build", "deploy"

    @Column(nullable = false)
    private String type;

    @Column(name = "depends_on", columnDefinition = "TEXT")
    @Convert(converter = JsonListConverter.class)
    private List<String> dependsOn = new ArrayList<>();

    @Column(name = "config", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private java.util.Map<String, Object> config = new java.util.HashMap<>();

    @Column(name = "retry_attempts")
    private Integer retryAttempts;

    @Column(name = "retry_delay_seconds")
    private Long retryDelaySeconds;

    @Column(name = "timeout_seconds")
    private Long timeoutSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    public TaskDefinitionEntity() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }

    public java.util.Map<String, Object> getConfig() { return config; }
    public void setConfig(java.util.Map<String, Object> config) { this.config = config; }

    public Integer getRetryAttempts() { return retryAttempts; }
    public void setRetryAttempts(Integer retryAttempts) { this.retryAttempts = retryAttempts; }

    public Long getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Long retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

    public Long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public WorkflowDefinitionEntity getWorkflowDefinition() { return workflowDefinition; }
    public void setWorkflowDefinition(WorkflowDefinitionEntity workflowDefinition) {
        this.workflowDefinition = workflowDefinition;
    }
}
