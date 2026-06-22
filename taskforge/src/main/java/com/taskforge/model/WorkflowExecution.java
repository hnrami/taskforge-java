package com.taskforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowExecution {

    private String id;
    private String workflowDefinitionId;
    private ExecutionStatus status;
    private List<TaskExecution> taskExecutions = new ArrayList<>();
    private Map<String, Object> outputs = new HashMap<>();
    private Instant startedAt;
    private Instant completedAt;

    public WorkflowExecution() {
    }

    public WorkflowExecution(String id, String workflowDefinitionId, ExecutionStatus status,
                             List<TaskExecution> taskExecutions, Map<String, Object> outputs,
                             Instant startedAt, Instant completedAt) {
        this.id = id;
        this.workflowDefinitionId = workflowDefinitionId;
        this.status = status;
        this.taskExecutions = taskExecutions;
        this.outputs = outputs;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(String workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public List<TaskExecution> getTaskExecutions() {
        return taskExecutions;
    }

    public void setTaskExecutions(List<TaskExecution> taskExecutions) {
        this.taskExecutions = taskExecutions;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
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
