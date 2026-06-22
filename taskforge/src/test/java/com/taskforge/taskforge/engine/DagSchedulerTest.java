package com.taskforge.engine;

import com.taskforge.model.TaskDefinition;
import com.taskforge.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DagSchedulerTest {

    private final DagScheduler scheduler = new DagScheduler();

    // ------------------------------------------------------------------
    // getReadyTasks
    // ------------------------------------------------------------------

    @Test
    void rootTask_noDepends_isReadyImmediately() {
        TaskDefinition A = task("A", List.of());
        Map<String, TaskStatus> statuses = Map.of("A", TaskStatus.PENDING);

        List<TaskDefinition> ready = scheduler.getReadyTasks(List.of(A), statuses);

        assertEquals(1, ready.size());
        assertEquals("A", ready.get(0).getId());
    }

    @Test
    void taskWithPendingDep_notReady() {
        TaskDefinition A = task("A", List.of());
        TaskDefinition B = task("B", List.of("A"));
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.PENDING);
        statuses.put("B", TaskStatus.PENDING);

        List<TaskDefinition> ready = scheduler.getReadyTasks(List.of(A, B), statuses);

        assertEquals(1, ready.size());
        assertEquals("A", ready.get(0).getId());
    }

    @Test
    void taskWithSuccessDep_isReady() {
        TaskDefinition B = task("B", List.of("A"));
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.SUCCESS);
        statuses.put("B", TaskStatus.PENDING);

        List<TaskDefinition> ready = scheduler.getReadyTasks(List.of(B), statuses);

        assertEquals(1, ready.size());
        assertEquals("B", ready.get(0).getId());
    }

    @Test
    void independentTasks_allReturnedAsReady() {
        TaskDefinition A = task("A", List.of());
        TaskDefinition B = task("B", List.of());
        TaskDefinition C = task("C", List.of());
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.PENDING);
        statuses.put("B", TaskStatus.PENDING);
        statuses.put("C", TaskStatus.PENDING);

        List<TaskDefinition> ready = scheduler.getReadyTasks(List.of(A, B, C), statuses);

        assertEquals(3, ready.size());
    }

    @Test
    void runningTask_notReturnedAsReady() {
        TaskDefinition A = task("A", List.of());
        Map<String, TaskStatus> statuses = Map.of("A", TaskStatus.RUNNING);

        List<TaskDefinition> ready = scheduler.getReadyTasks(List.of(A), statuses);

        assertTrue(ready.isEmpty());
    }

    // ------------------------------------------------------------------
    // isFinished
    // ------------------------------------------------------------------

    @Test
    void isFinished_whenAllTasksTerminal() {
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.SUCCESS);
        statuses.put("B", TaskStatus.FAILED);
        statuses.put("C", TaskStatus.SKIPPED);
        statuses.put("D", TaskStatus.CANCELLED);

        assertTrue(scheduler.isFinished(statuses));
    }

    @Test
    void isNotFinished_whenPendingTaskExists() {
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.SUCCESS);
        statuses.put("B", TaskStatus.PENDING);

        assertFalse(scheduler.isFinished(statuses));
    }

    @Test
    void isNotFinished_whenRunningTaskExists() {
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.RUNNING);

        assertFalse(scheduler.isFinished(statuses));
    }

    // ------------------------------------------------------------------
    // hasFailed
    // ------------------------------------------------------------------

    @Test
    void hasFailed_whenFailedTaskExists() {
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.SUCCESS);
        statuses.put("B", TaskStatus.FAILED);

        assertTrue(scheduler.hasFailed(statuses));
    }

    @Test
    void hasNotFailed_whenAllSuccess() {
        Map<String, TaskStatus> statuses = new HashMap<>();
        statuses.put("A", TaskStatus.SUCCESS);
        statuses.put("B", TaskStatus.SUCCESS);

        assertFalse(scheduler.hasFailed(statuses));
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private TaskDefinition task(String id, List<String> deps) {
        TaskDefinition td = new TaskDefinition();
        td.setId(id);
        td.setType("notification");
        td.setDependsOn(new ArrayList<>(deps));
        return td;
    }
}
