package com.taskforge.model;

import java.util.HashMap;
import java.util.Map;

public class TaskResult {

    private boolean success;
    private Map<String, Object> outputs = new HashMap<>();
    private String errorMessage;
    private boolean retryable;

    public TaskResult() {
    }

    public TaskResult(boolean success, Map<String, Object> outputs, String errorMessage, boolean retryable) {
        this.success = success;
        this.outputs = outputs;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }
}
