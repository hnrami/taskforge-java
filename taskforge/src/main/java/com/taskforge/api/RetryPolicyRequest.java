package com.taskforge.api;

public class RetryPolicyRequest {
    
    private int attempts;
    private long delaySeconds;

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public long getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(long delaySeconds) { this.delaySeconds = delaySeconds; }
}
