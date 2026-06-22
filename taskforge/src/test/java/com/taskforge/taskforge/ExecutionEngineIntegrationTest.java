package com.taskforge;

import com.taskforge.api.*;
import com.taskforge.handler.TaskHandler;
import com.taskforge.model.TaskContext;
import com.taskforge.model.TaskResult;
import com.taskforge.service.ExecutionService;
import com.taskforge.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full integration test for ExecutionEngine behaviours.
 *
 * Custom handlers are registered via @TestConfiguration.
 * The ExecutionEngine, DagScheduler, StateManager are completely unmodified.
 * This test class itself proves the Open/Closed principle:
 * new task types added here required ZERO changes to engine code.
 */
@SpringBootTest
class ExecutionEngineIntegrationTest {

    // =========================================================================
    // Test-only task handlers — registered through Spring like any real handler
    // =========================================================================

    @TestConfiguration
    static class TestHandlers {

        /** Always fails, non-retryable */
        @Bean("alwaysFailHandler")
        public TaskHandler alwaysFailHandler() {
            return new TaskHandler() {
                @Override public String type() { return "always-fail"; }
                @Override public TaskResult execute(TaskContext ctx) {
                    return failure("Intentional failure", false);
                }
                @Override public void cancel() {}
            };
        }

        /** Always fails with retryable=true */
        @Bean("alwaysFailRetryableHandler")
        public TaskHandler alwaysFailRetryableHandler() {
            return new TaskHandler() {
                @Override public String type() { return "always-fail-retryable"; }
                @Override public TaskResult execute(TaskContext ctx) {
                    return failure("Retryable failure", true);
                }
                @Override public void cancel() {}
            };
        }

        @Bean
        public FailTwiceThenSucceedHandler failTwiceThenSucceedHandler() {
            return new FailTwiceThenSucceedHandler();
        }

        @Bean
        public SlowTaskTestHandler slowTaskTestHandler() {
            return new SlowTaskTestHandler();
        }

        @Bean
        public ParallelTrackingHandler parallelTrackingHandler() {
            return new ParallelTrackingHandler();
        }

        /**
         * The extensibility test handler.
         * Type "test-ext" was never mentioned anywhere in engine code.
         * Registering this bean is sufficient to make it work — zero engine changes.
         */
        @Bean("testExtHandler")
        public TaskHandler testExtHandler() {
            return new TaskHandler() {
                @Override public String type() { return "test-ext"; }
                @Override public TaskResult execute(TaskContext ctx) {
                    TaskResult r = new TaskResult();
                    r.setSuccess(true);
                    r.setOutputs(Map.of("result", "test-ext-executed"));
                    return r;
                }
                @Override public void cancel() {}
            };
        }

        private static TaskResult failure(String msg, boolean retryable) {
            TaskResult r = new TaskResult();
            r.setSuccess(false);
            r.setErrorMessage(msg);
            r.setRetryable(retryable);
            return r;
        }
    }

    // -------------------------------------------------------------------------
    // Stateful test handler implementations
    // -------------------------------------------------------------------------

    /** Fails the first 2 invocations, then succeeds on attempt 3. */
    static class FailTwiceThenSucceedHandler implements TaskHandler {
        private final AtomicInteger calls = new AtomicInteger(0);

        @Override public String type() { return "fail-twice"; }

        @Override
        public TaskResult execute(TaskContext ctx) {
            int attempt = calls.incrementAndGet();
            if (attempt <= 2) {
                TaskResult r = new TaskResult();
                r.setSuccess(false);
                r.setErrorMessage("Planned failure on attempt " + attempt);
                r.setRetryable(true);
                return r;
            }
            TaskResult r = new TaskResult();
            r.setSuccess(true);
            return r;
        }

        @Override public void cancel() {}
        public void reset() { calls.set(0); }
    }

    /** Sleeps 60 s — used for timeout and cancellation tests. */
    static class SlowTaskTestHandler implements TaskHandler {
        private volatile Thread thread;

        @Override public String type() { return "slow-task"; }

        @Override
        public TaskResult execute(TaskContext ctx) {
            thread = Thread.currentThread();
            try {
                Thread.sleep(60_000);
                TaskResult r = new TaskResult();
                r.setSuccess(true);
                return r;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                TaskResult r = new TaskResult();
                r.setSuccess(false);
                r.setErrorMessage("Interrupted");
                r.setRetryable(false);
                return r;
            } finally {
                thread = null;
            }
        }

        @Override
        public void cancel() {
            Thread t = thread;
            if (t != null) t.interrupt();
        }
    }

    /** Records max concurrent executions to prove parallel scheduling. */
    static class ParallelTrackingHandler implements TaskHandler {
        private final AtomicInteger concurrent = new AtomicInteger(0);
        private volatile int maxConcurrent = 0;

        @Override public String type() { return "parallel-track"; }

        @Override
        public TaskResult execute(TaskContext ctx) {
            int c = concurrent.incrementAndGet();
            synchronized (this) {
                if (c > maxConcurrent) maxConcurrent = c;
            }
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            concurrent.decrementAndGet();
            TaskResult r = new TaskResult();
            r.setSuccess(true);
            return r;
        }

        @Override public void cancel() {}

        public int getMaxConcurrent() { return maxConcurrent; }
        public void reset() { concurrent.set(0); maxConcurrent = 0; }
    }

    // =========================================================================
    // Autowired beans
    // =========================================================================

    @Autowired private WorkflowService workflowService;
    @Autowired private ExecutionService executionService;
    @Autowired private FailTwiceThenSucceedHandler failTwiceThenSucceedHandler;
    @Autowired private ParallelTrackingHandler parallelTrackingHandler;

    @BeforeEach
    void resetHandlerState() {
        failTwiceThenSucceedHandler.reset();
        parallelTrackingHandler.reset();
    }

    // =========================================================================
    // Tests
    // =========================================================================

    // ------------------------------------------------------------------
    // 1. Successful linear workflow
    // ------------------------------------------------------------------

    @Test
    void successfulWorkflow_allTasksSucceed() throws Exception {
        WorkflowResponse wf = create("success-linear", List.of(
                req("A", "notification", List.of()),
                req("B", "notification", List.of("A")),
                req("C", "notification", List.of("B"))
        ));

        ExecutionResponse result = runAndWait(wf.getId());

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("SUCCESS", result.getTasks().get("A"));
        assertEquals("SUCCESS", result.getTasks().get("B"));
        assertEquals("SUCCESS", result.getTasks().get("C"));
    }

    // ------------------------------------------------------------------
    // 2. Task failure — downstream tasks are SKIPPED
    // ------------------------------------------------------------------

    @Test
    void taskFailure_downstreamTasksAreSkipped() throws Exception {
        WorkflowResponse wf = create("fail-skip", List.of(
                req("A", "notification", List.of()),
                req("B", "always-fail",  List.of("A")),
                req("C", "notification", List.of("B"))
        ));

        ExecutionResponse result = runAndWait(wf.getId());

        assertEquals("FAILED",   result.getStatus());
        assertEquals("SUCCESS",  result.getTasks().get("A"));
        assertEquals("FAILED",   result.getTasks().get("B"));
        assertEquals("SKIPPED",  result.getTasks().get("C"));
    }

    // ------------------------------------------------------------------
    // 3. Retry success — task succeeds on 3rd attempt
    // ------------------------------------------------------------------

    @Test
    void retrySuccess_taskSucceedsAfterTwoFailures() throws Exception {
        WorkflowResponse wf = create("retry-success", List.of(
                reqWithRetry("A", "fail-twice", List.of(), 3, 0)
        ));

        ExecutionResponse result = runAndWait(wf.getId());

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("SUCCESS", result.getTasks().get("A"));
    }

    // ------------------------------------------------------------------
    // 4. Retry exhausted — all attempts fail
    // ------------------------------------------------------------------

    @Test
    void retryExhausted_workflowFails() throws Exception {
        WorkflowResponse wf = create("retry-exhausted", List.of(
                reqWithRetry("A", "always-fail-retryable", List.of(), 3, 0)
        ));

        ExecutionResponse result = runAndWait(wf.getId());

        assertEquals("FAILED", result.getStatus());
        assertEquals("FAILED", result.getTasks().get("A"));
    }

    // ------------------------------------------------------------------
    // 5. Timeout — task exceeds timeout, marked FAILED
    // ------------------------------------------------------------------

    @Test
    void taskTimeout_executionFails() throws Exception {
        WorkflowResponse wf = create("timeout-test", List.of(
                reqWithTimeout("A", "slow-task", List.of(), 1L)   // 1-second timeout
        ));

        ExecutionResponse result = runAndWait(wf.getId(), Duration.ofSeconds(15));

        assertEquals("FAILED", result.getStatus());
        assertEquals("FAILED", result.getTasks().get("A"));
    }

    // ------------------------------------------------------------------
    // 6. Cancellation
    // ------------------------------------------------------------------

    @Test
    void cancellation_stopsRunningAndCancelsPendingTasks() throws Exception {
        WorkflowResponse wf = create("cancel-test", List.of(
                req("A", "notification", List.of()),
                req("B", "slow-task",    List.of("A")),
                req("C", "notification", List.of("B"))
        ));

        ExecutionResponse exec = executionService.startExecution(wf.getId());

        // Wait until B starts running (blocks in SlowTaskTestHandler)
        waitForTaskStatus(exec.getId(), "B", "RUNNING", Duration.ofSeconds(10));

        // Cancel
        executionService.cancelExecution(exec.getId());

        ExecutionResponse result = waitForTerminal(exec.getId(), Duration.ofSeconds(10));

        assertEquals("CANCELLED", result.getStatus());
        assertEquals("SUCCESS",   result.getTasks().get("A"));
        assertNotEquals("SUCCESS", result.getTasks().get("B"));    // interrupted
        assertEquals("CANCELLED", result.getTasks().get("C"));     // never started
    }

    // ------------------------------------------------------------------
    // 7. Parallel execution — B and C run concurrently
    // ------------------------------------------------------------------

    @Test
    void parallelExecution_independentTasksRunConcurrently() throws Exception {
        WorkflowResponse wf = create("parallel-test", List.of(
                req("A", "notification",    List.of()),
                req("B", "parallel-track",  List.of("A")),
                req("C", "parallel-track",  List.of("A")),
                req("D", "notification",    List.of("B", "C"))
        ));

        ExecutionResponse result = runAndWait(wf.getId(), Duration.ofSeconds(20));

        assertEquals("SUCCESS", result.getStatus());
        // If maxConcurrent >= 2 then B and C ran simultaneously
        assertTrue(parallelTrackingHandler.getMaxConcurrent() >= 2,
                "Expected B and C to run concurrently but maxConcurrent="
                        + parallelTrackingHandler.getMaxConcurrent());
    }

    // ------------------------------------------------------------------
    // 8. Approval flow — workflow pauses until approved
    // ------------------------------------------------------------------

    @Test
    void approvalFlow_workflowPausesAndContinuesAfterApproval() throws Exception {
        WorkflowResponse wf = create("approval-test", List.of(
                req("build",  "notification", List.of()),
                reqWithConfig("gate", "approval", List.of("build"),
                        Map.of("timeoutMinutes", "5")),
                req("deploy", "notification", List.of("gate"))
        ));

        ExecutionResponse exec = executionService.startExecution(wf.getId());

        // Wait for approval task to block (status RUNNING = blocked on latch)
        waitForTaskStatus(exec.getId(), "gate", "RUNNING", Duration.ofSeconds(10));

        // Verify deploy has NOT started while approval is pending
        ExecutionResponse mid = executionService.getExecution(exec.getId());
        assertNotEquals("SUCCESS", mid.getTasks().get("deploy"),
                "Deploy should not start before approval is given");

        // Approve
        ApprovalRequest approval = new ApprovalRequest();
        approval.setTaskId("gate");
        approval.setApproved(true);
        approval.setApprover("test-reviewer");
        executionService.resolveApproval(exec.getId(), approval);

        ExecutionResponse result = waitForTerminal(exec.getId(), Duration.ofSeconds(15));

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("SUCCESS", result.getTasks().get("build"));
        assertEquals("SUCCESS", result.getTasks().get("gate"));
        assertEquals("SUCCESS", result.getTasks().get("deploy"));
    }

    // ------------------------------------------------------------------
    // 9. Approval rejected — workflow fails
    // ------------------------------------------------------------------

    @Test
    void approvalRejected_workflowFails() throws Exception {
        WorkflowResponse wf = create("approval-reject-test", List.of(
                req("build", "notification", List.of()),
                reqWithConfig("gate", "approval", List.of("build"), Map.of("timeoutMinutes", "5")),
                req("deploy", "notification", List.of("gate"))
        ));

        ExecutionResponse exec = executionService.startExecution(wf.getId());
        waitForTaskStatus(exec.getId(), "gate", "RUNNING", Duration.ofSeconds(10));

        ApprovalRequest rejection = new ApprovalRequest();
        rejection.setTaskId("gate");
        rejection.setApproved(false);
        rejection.setApprover("security-lead");
        rejection.setReason("Not safe to deploy");
        executionService.resolveApproval(exec.getId(), rejection);

        ExecutionResponse result = waitForTerminal(exec.getId(), Duration.ofSeconds(10));

        assertEquals("FAILED",  result.getStatus());
        assertEquals("FAILED",  result.getTasks().get("gate"));
        assertEquals("SKIPPED", result.getTasks().get("deploy"));
    }

    // ------------------------------------------------------------------
    // 10. EXTENSIBILITY — new handler type, zero engine changes
    // ------------------------------------------------------------------

    /**
     * This is the most important test.
     *
     * The "test-ext" TaskHandler was defined ONLY in the @TestConfiguration above.
     * No changes were made to:
     *   - ExecutionEngine
     *   - DagScheduler
     *   - StateManager
     *   - TaskHandlerRegistry
     *   - Any existing handler
     *
     * The engine picked up "test-ext" automatically via ObjectProvider<TaskHandler>.
     * This proves the Open/Closed principle: open for extension, closed for modification.
     */
    @Test
    void extensibility_newHandlerRunsWithoutAnyEngineChanges() throws Exception {
        WorkflowResponse wf = create("extensibility-test", List.of(
                req("A", "notification", List.of()),
                req("B", "test-ext",     List.of("A")),
                req("C", "notification", List.of("B"))
        ));

        ExecutionResponse result = runAndWait(wf.getId());

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("SUCCESS", result.getTasks().get("A"));
        assertEquals("SUCCESS", result.getTasks().get("B"));
        assertEquals("SUCCESS", result.getTasks().get("C"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private WorkflowResponse create(String name, List<TaskRequest> tasks) {
        CreateWorkflowRequest req = new CreateWorkflowRequest();
        req.setName(name + "-" + System.nanoTime());
        req.setTasks(tasks);
        return workflowService.createWorkflow(req);
    }

    private TaskRequest req(String id, String type, List<String> deps) {
        TaskRequest r = new TaskRequest();
        r.setId(id);
        r.setType(type);
        r.setDependsOn(new ArrayList<>(deps));
        r.setConfig(new HashMap<>());
        return r;
    }

    private TaskRequest reqWithRetry(String id, String type, List<String> deps, int attempts, long delay) {
        TaskRequest r = req(id, type, deps);
        RetryPolicyRequest rp = new RetryPolicyRequest();
        rp.setAttempts(attempts);
        rp.setDelaySeconds(delay);
        r.setRetryPolicy(rp);
        return r;
    }

    private TaskRequest reqWithTimeout(String id, String type, List<String> deps, long timeoutSeconds) {
        TaskRequest r = req(id, type, deps);
        r.setTimeoutSeconds(timeoutSeconds);
        return r;
    }

    private TaskRequest reqWithConfig(String id, String type, List<String> deps, Map<String, Object> config) {
        TaskRequest r = req(id, type, deps);
        r.setConfig(config);
        return r;
    }

    /**
     * Starts a new execution for the given workflow ID and polls until terminal.
     */
    private ExecutionResponse runAndWait(String workflowId) throws InterruptedException {
        return runAndWait(workflowId, Duration.ofSeconds(15));
    }

    private ExecutionResponse runAndWait(String workflowId, Duration timeout) throws InterruptedException {
        ExecutionResponse exec = executionService.startExecution(workflowId);
        return waitForTerminal(exec.getId(), timeout);
    }

    private ExecutionResponse waitForTerminal(String executionId, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            Thread.sleep(150);
            ExecutionResponse resp = executionService.getExecution(executionId);
            String status = resp.getStatus();
            if ("SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status)) {
                return resp;
            }
        }
        ExecutionResponse last = executionService.getExecution(executionId);
        fail("Execution " + executionId + " did not complete within " + timeout
                + ". Last status: " + last.getStatus() + ", tasks: " + last.getTasks());
        return null;
    }

    private void waitForTaskStatus(String execId, String taskId,
                                   String expectedStatus, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            Thread.sleep(100);
            ExecutionResponse resp = executionService.getExecution(execId);
            if (expectedStatus.equals(resp.getTasks().get(taskId))) {
                return;
            }
        }
        ExecutionResponse last = executionService.getExecution(execId);
        fail("Task '" + taskId + "' did not reach status '" + expectedStatus
                + "' within " + timeout + ". Last task statuses: " + last.getTasks());
    }
}
