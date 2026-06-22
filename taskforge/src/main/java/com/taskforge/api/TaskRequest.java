package com.taskforge.api;

import java.util.List;
import java.util.Map;

public class TaskRequest {
    
    private String id;
    private String type;
    private List<String> dependsOn;
    private Map<String, Object> config;
    private RetryPolicyRequest retryPolicy;
    private Long timeoutSeconds;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public RetryPolicyRequest getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(RetryPolicyRequest retryPolicy) { this.retryPolicy = retryPolicy; }

    public Long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}

