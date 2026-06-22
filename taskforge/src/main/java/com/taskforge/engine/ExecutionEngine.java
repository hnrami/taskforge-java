package com.taskforge.engine;

import com.taskforge.handler.TaskHandler;
import com.taskforge.handler.TaskHandlerRegistry;
import com.taskforge.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * ExecutionEngine orchestrates a workflow execution.
 *
 * Design contract (from requirements):
 *  - Engine NEVER knows concrete task types.
 *  - No if/else or switch on task type.
 *  - All task execution goes through TaskHandler interface via TaskHandlerRegistry.
 *  - Adding new task types requires zero engine changes.
 */
@Component
public class ExecutionEngine {

    private final TaskHandlerRegistry handlerRegistry;
    private final DagScheduler dagScheduler;
    private final StateManager stateManager;
    private final ExecutorService taskParallelExecutor;

    // Per-execution cancellation flags and running handler references
    private final ConcurrentHashMap<String, AtomicBoolean> cancellationFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<TaskHandler>> runningHandlers = new ConcurrentHashMap<>();

    public ExecutionEngine(TaskHandlerRegistry handlerRegistry,
                           DagScheduler dagScheduler,
                           StateManager stateManager,
                           @Qualifier("taskParallelExecutor") ExecutorService taskParallelExecutor) {
        this.handlerRegistry = handlerRegistry;
        this.dagScheduler = dagScheduler;
        this.stateManager = stateManager;
        this.taskParallelExecutor = taskParallelExecutor;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Starts a workflow execution asynchronously.
     * Must be called from a DIFFERENT Spring bean (e.g. WorkflowService) for
     * the @Async proxy to take effect.
     *
     * @param workflowExecutionId ID of the already-persisted WorkflowExecutionEntity
     * @param workflowDef         plain-model workflow definition
     */
    @Async("executionEngineExecutor")
    public void startExecution(String workflowExecutionId, WorkflowDefinition workflowDef) {
        cancellationFlags.put(workflowExecutionId, new AtomicBoolean(false));
        runningHandlers.put(workflowExecutionId, Collections.synchronizedList(new ArrayList<>()));

        try {
            stateManager.initializeTaskExecutions(workflowExecutionId, workflowDef.getTasks());
            stateManager.updateWorkflowStatus(workflowExecutionId, ExecutionStatus.RUNNING);
            runLoop(workflowExecutionId, workflowDef);
        } finally {
            cancellationFlags.remove(workflowExecutionId);
            runningHandlers.remove(workflowExecutionId);
        }
    }

    /**
     * Requests cancellation of an in-progress execution.
     * Pending tasks are marked CANCELLED; running task handlers receive cancel().
     */
    public void requestCancellation(String workflowExecutionId) {
        AtomicBoolean flag = cancellationFlags.get(workflowExecutionId);
        if (flag != null) {
            flag.set(true);
        }
        List<TaskHandler> handlers = runningHandlers.getOrDefault(workflowExecutionId, Collections.emptyList());
        synchronized (handlers) {
            new ArrayList<>(handlers).forEach(TaskHandler::cancel);
        }
    }

    // =========================================================================
    // Execution loop
    // =========================================================================

    private void runLoop(String workflowExecutionId, WorkflowDefinition workflowDef) {
        while (true) {

            if (isCancelled(workflowExecutionId)) {
                performCancellation(workflowExecutionId, workflowDef);
                return;
            }

            Map<String, TaskStatus> statuses = stateManager.loadTaskStatuses(workflowExecutionId);

            // Mark tasks whose upstream has failed/cancelled/skipped as SKIPPED
            skipUnreachableTasks(workflowExecutionId, workflowDef, statuses);

            // Reload after potential skips
            statuses = stateManager.loadTaskStatuses(workflowExecutionId);

            if (dagScheduler.isFinished(statuses)) {
                ExecutionStatus finalStatus = dagScheduler.hasFailed(statuses)
                        ? ExecutionStatus.FAILED
                        : ExecutionStatus.SUCCESS;
                stateManager.updateWorkflowStatus(workflowExecutionId, finalStatus);
                return;
            }

            List<TaskDefinition> readyTasks = dagScheduler.getReadyTasks(workflowDef.getTasks(), statuses);

            if (readyTasks.isEmpty()) {
                // No tasks ready: either waiting for approval or all tasks running — poll
                sleep(300);
                continue;
            }

            // Atomically mark all ready tasks as RUNNING to prevent double-scheduling
            readyTasks.forEach(t ->
                    stateManager.updateTaskStatus(workflowExecutionId, t.getId(), TaskStatus.RUNNING));

            // Execute the ready wave in parallel; block until wave completes
            executeParallel(readyTasks, workflowExecutionId);
        }
    }

    // =========================================================================
    // Parallel execution
    // =========================================================================

    private void executeParallel(List<TaskDefinition> tasks, String workflowExecutionId) {
        List<Future<?>> futures = tasks.stream()
                .map(task -> taskParallelExecutor
                        .submit(() -> executeTask(task, workflowExecutionId)))
                .collect(Collectors.toList());

        // Wait for all parallel tasks; break early on cancellation
        while (!allDone(futures)) {
            if (isCancelled(workflowExecutionId)) {
                futures.forEach(f -> f.cancel(true));
                return;
            }
            sleep(100);
        }
    }

    // =========================================================================
    // Single task execution with retry + timeout
    // =========================================================================

    private void executeTask(TaskDefinition task, String workflowExecutionId) {
        TaskHandler handler = handlerRegistry.get(task.getType());

        List<TaskHandler> handlers = runningHandlers.getOrDefault(workflowExecutionId, Collections.emptyList());
        synchronized (handlers) { handlers.add(handler); }

        try {
            Map<String, Object> sharedOutputs = stateManager.loadSharedOutputs(workflowExecutionId);
            TaskContext context = new TaskContext(
                    workflowExecutionId, task.getId(), task.getConfig(), sharedOutputs);

            int maxAttempts = (task.getRetryPolicy() != null) ? task.getRetryPolicy().getAttempts() : 1;
            long delayMs   = (task.getRetryPolicy() != null) ? task.getRetryPolicy().getDelaySeconds() * 1000L : 0L;
            Long timeoutSecs = task.getTimeoutSeconds();

            String lastError = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    TaskResult result = (timeoutSecs != null && timeoutSecs > 0)
                            ? executeWithTimeout(handler, context, timeoutSecs)
                            : handler.execute(context);

                    if (result.isSuccess()) {
                        stateManager.saveTaskOutputs(workflowExecutionId, task.getId(), result.getOutputs());
                        stateManager.updateTaskStatusAndAttempt(
                                workflowExecutionId, task.getId(), TaskStatus.SUCCESS, attempt, null);
                        return;
                    }

                    lastError = result.getErrorMessage();
                    if (!result.isRetryable() || attempt == maxAttempts) {
                        break;
                    }

                } catch (TimeoutException e) {
                    handler.cancel();
                    stateManager.updateTaskStatusAndAttempt(
                            workflowExecutionId, task.getId(), TaskStatus.FAILED, attempt,
                            "Task timed out after " + timeoutSecs + "s");
                    return;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stateManager.updateTaskStatusAndAttempt(
                            workflowExecutionId, task.getId(), TaskStatus.CANCELLED, attempt,
                            "Task interrupted");
                    return;

                } catch (ExecutionException e) {
                    lastError = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                    if (attempt == maxAttempts) break;

                } catch (Exception e) {
                    lastError = e.getMessage();
                    if (attempt == maxAttempts) break;
                }

                if (delayMs > 0) sleep(delayMs);
            }

            stateManager.updateTaskStatusAndAttempt(
                    workflowExecutionId, task.getId(), TaskStatus.FAILED, maxAttempts, lastError);

        } finally {
            synchronized (handlers) { handlers.remove(handler); }
        }
    }

    /**
     * Runs handler.execute() with an enforced timeout via a dedicated Future.
     */
    private TaskResult executeWithTimeout(TaskHandler handler, TaskContext context, long timeoutSeconds)
            throws TimeoutException, InterruptedException, ExecutionException {

        Future<TaskResult> future = taskParallelExecutor
                .submit(() -> handler.execute(context));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    // =========================================================================
    // Cancellation
    // =========================================================================

    private void performCancellation(String workflowExecutionId, WorkflowDefinition workflowDef) {
        Map<String, TaskStatus> statuses = stateManager.loadTaskStatuses(workflowExecutionId);
        statuses.forEach((taskId, status) -> {
            if (status == TaskStatus.PENDING || status == TaskStatus.WAITING_APPROVAL) {
                stateManager.updateTaskStatus(workflowExecutionId, taskId, TaskStatus.CANCELLED);
            }
        });
        stateManager.updateWorkflowStatus(workflowExecutionId, ExecutionStatus.CANCELLED);
    }

    // =========================================================================
    // Skip unreachable tasks
    // =========================================================================

    /**
     * Tasks whose declared dependencies are in a terminal-failure state can never run.
     * Mark them SKIPPED so the loop eventually terminates.
     */
    private void skipUnreachableTasks(String workflowExecutionId,
                                      WorkflowDefinition workflowDef,
                                      Map<String, TaskStatus> statuses) {
        Set<String> terminated = statuses.entrySet().stream()
                .filter(e -> e.getValue() == TaskStatus.FAILED
                        || e.getValue() == TaskStatus.SKIPPED
                        || e.getValue() == TaskStatus.CANCELLED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (TaskDefinition task : workflowDef.getTasks()) {
            if (statuses.getOrDefault(task.getId(), TaskStatus.PENDING) != TaskStatus.PENDING) {
                continue;
            }
            boolean hasBlockedDep = task.getDependsOn() != null
                    && task.getDependsOn().stream().anyMatch(terminated::contains);
            if (hasBlockedDep) {
                stateManager.updateTaskStatus(workflowExecutionId, task.getId(), TaskStatus.SKIPPED);
            }
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private boolean isCancelled(String workflowExecutionId) {
        AtomicBoolean flag = cancellationFlags.get(workflowExecutionId);
        return flag != null && flag.get();
    }

    private boolean allDone(List<Future<?>> futures) {
        return futures.stream().allMatch(Future::isDone);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
