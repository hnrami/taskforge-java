package com.taskforge.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class WorkflowResponse {

    private String id;
    private String name;
    private List<TaskSummary> tasks;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<TaskSummary> getTasks() { return tasks; }
    public void setTasks(List<TaskSummary> tasks) { this.tasks = tasks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class TaskSummary {
        private String taskId;
        private String type;
        private List<String> dependsOn;
        private Map<String, Object> config;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<String> getDependsOn() { return dependsOn; }
        public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }

        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
}

