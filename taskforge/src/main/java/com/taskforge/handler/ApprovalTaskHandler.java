package com.taskforge.handler;

import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Approval task — pauses workflow until an external API call provides a decision.
 *
 * The execution engine marks this task as WAITING_APPROVAL via the status returned
 * from this handler blocking until:
 *   (a) ApprovalTaskHandler.resolve() is called externally (by ExecutionService), or
 *   (b) The configured timeout elapses (default 24h), or
 *   (c) cancel() is called.
 *
 * Config keys:
 *   timeoutMinutes (long, default 1440 = 24 h)
 *
 * Output: approved (boolean), approver (String), reason (String)
 */
@Component
public class ApprovalTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskHandler.class);

    /**
     * Key: workflowExecutionId + ":" + taskId
     * Value: pending approval latch and result
     */
    private final ConcurrentHashMap<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "approval";
    }

    @Override
    public TaskResult execute(TaskContext context) {
        String key = approvalKey(context.getWorkflowExecutionId(), context.getTaskId());
        long timeoutMinutes = parseTimeout(context.getTaskConfig());

        PendingApproval pending = new PendingApproval();
        pendingApprovals.put(key, pending);
        log.info("[ApprovalTaskHandler] Waiting for approval: execution={} task={}",
                context.getWorkflowExecutionId(), context.getTaskId());

        try {
            boolean resolved = pending.latch.await(timeoutMinutes, TimeUnit.MINUTES);

            if (!resolved || pending.cancelled.get()) {
                return failure("Approval timed out or was cancelled", false);
            }

            Map<String, Object> outputs = new HashMap<>();
            outputs.put("approved", pending.approved);
            outputs.put("approver", pending.approver);
            outputs.put("reason", pending.reason);

            if (pending.approved) {
                TaskResult result = new TaskResult();
                result.setSuccess(true);
                result.setOutputs(outputs);
                return result;
            } else {
                TaskResult result = new TaskResult();
                result.setSuccess(false);
                result.setErrorMessage("Approval rejected by " + pending.approver
                        + (pending.reason != null ? ": " + pending.reason : ""));
                result.setOutputs(outputs);
                result.setRetryable(false);
                return result;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failure("Approval wait interrupted", true);
        } finally {
            pendingApprovals.remove(key);
        }
    }

    /**
     * Called by ExecutionService when the approval API endpoint is hit.
     */
    public boolean resolve(String workflowExecutionId, String taskId,
                           boolean approved, String approver, String reason) {
        String key = approvalKey(workflowExecutionId, taskId);
        PendingApproval pending = pendingApprovals.get(key);
        if (pending == null) {
            return false;   // no pending approval for this key
        }
        pending.approved = approved;
        pending.approver = (approver != null) ? approver : "unknown";
        pending.reason   = reason;
        pending.latch.countDown();
        return true;
    }

    @Override
    public void cancel() {
        pendingApprovals.forEach((key, pending) -> {
            pending.cancelled.set(true);
            pending.latch.countDown();
        });
    }

    // -------------------------------------------------------------------------

    private String approvalKey(String workflowExecutionId, String taskId) {
        return workflowExecutionId + ":" + taskId;
    }

    private long parseTimeout(Map<String, Object> config) {
        Object raw = config.get("timeoutMinutes");
        if (raw == null) return 1440L;
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return 1440L;
        }
    }

    private TaskResult failure(String error, boolean retryable) {
        TaskResult r = new TaskResult();
        r.setSuccess(false);
        r.setErrorMessage(error);
        r.setRetryable(retryable);
        return r;
    }

    // -------------------------------------------------------------------------

    private static class PendingApproval {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        volatile boolean approved = false;
        volatile String approver;
        volatile String reason;
    }
}
