package com.taskforge.api;

import java.time.Instant;
import java.util.Map;

public class ExecutionResponse {
    
private String id;
    private String workflowDefinitionId;
    private String status;
    private Map<String, String> tasks;   // taskId -> TaskStatus name
    private Instant startedAt;
    private Instant completedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowDefinitionId() { return workflowDefinitionId; }
    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, String> getTasks() { return tasks; }
    public void setTasks(Map<String, String> tasks) { this.tasks = tasks; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
