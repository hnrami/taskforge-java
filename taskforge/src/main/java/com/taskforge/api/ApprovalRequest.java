package com.taskforge.api;

public class ApprovalRequest {

    private String taskId;
    private boolean approved;
    private String approver;
    private String reason;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
