package com.taskforge.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskDefinition {

    private String id;
    private String type;
    private List<String> dependsOn = new ArrayList<>();
    private Map<String, Object> config = new HashMap<>();
    private RetryPolicy retryPolicy;
    private Long timeoutSeconds;

    public TaskDefinition() {
    }

    public TaskDefinition(String id, String type, List<String> dependsOn, Map<String, Object> config,
                          RetryPolicy retryPolicy, Long timeoutSeconds) {
        this.id = id;
        this.type = type;
        this.dependsOn = dependsOn;
        this.config = config;
        this.retryPolicy = retryPolicy;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
