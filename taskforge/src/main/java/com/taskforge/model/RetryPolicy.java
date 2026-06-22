package com.taskforge.model;

public class RetryPolicy {

    private int attempts;
    private long delaySeconds;

    public RetryPolicy() {
    }

    public RetryPolicy(int attempts, long delaySeconds) {
        this.attempts = attempts;
        this.delaySeconds = delaySeconds;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(long delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}
