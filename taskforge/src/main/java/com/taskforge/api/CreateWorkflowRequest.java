package com.taskforge.api;

import java.util.List;

public class CreateWorkflowRequest {

    private String name;
    private List<TaskRequest> tasks;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<TaskRequest> getTasks() { return tasks; }
    public void setTasks(List<TaskRequest> tasks) { this.tasks = tasks; }
    
}
