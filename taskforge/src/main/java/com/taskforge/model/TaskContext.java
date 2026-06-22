package com.taskforge.model;

import java.util.HashMap;
import java.util.Map;

public class TaskContext {

    private String workflowExecutionId;
    private String taskId;
    private Map<String, Object> taskConfig = new HashMap<>();
    private Map<String, Object> sharedOutputs = new HashMap<>();

    public TaskContext() {
    }

    public TaskContext(String workflowExecutionId, String taskId, Map<String, Object> taskConfig,
                       Map<String, Object> sharedOutputs) {
        this.workflowExecutionId = workflowExecutionId;
        this.taskId = taskId;
        this.taskConfig = taskConfig;
        this.sharedOutputs = sharedOutputs;
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

    public Map<String, Object> getTaskConfig() {
        return taskConfig;
    }

    public void setTaskConfig(Map<String, Object> taskConfig) {
        this.taskConfig = taskConfig;
    }

    public Map<String, Object> getSharedOutputs() {
        return sharedOutputs;
    }

    public void setSharedOutputs(Map<String, Object> sharedOutputs) {
        this.sharedOutputs = sharedOutputs;
    }
}
