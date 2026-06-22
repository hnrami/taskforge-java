package com.taskforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WorkflowDefinition {

    private String id;
    private String name;
    private List<TaskDefinition> tasks = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public WorkflowDefinition() {
    }

    public WorkflowDefinition(String id, String name, List<TaskDefinition> tasks, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.tasks = tasks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TaskDefinition> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDefinition> tasks) {
        this.tasks = tasks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
