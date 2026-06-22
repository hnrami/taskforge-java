package com.taskforge.api;

import com.taskforge.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExecutionController {

     private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * POST /api/workflows/{id}/executions
     * Starts a new execution for the given workflow.
     */
    @PostMapping("/api/workflows/{id}/executions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionResponse startExecution(@PathVariable String id) {
        return executionService.startExecution(id);
    }

    /**
     * GET /api/executions/{id}
     * Returns current execution status and per-task states.
     */
    @GetMapping("/api/executions/{id}")
    public ExecutionResponse getExecution(@PathVariable String id) {
        return executionService.getExecution(id);
    }

    /**
     * POST /api/executions/{id}/cancel
     * Requests graceful cancellation of a running execution.
     */
    @PostMapping("/api/executions/{id}/cancel")
    public ResponseEntity<Void> cancelExecution(@PathVariable String id) {
        executionService.cancelExecution(id);
        return ResponseEntity.accepted().build();
    }

    /**
     * POST /api/executions/{id}/approval
     * Provides an approval decision for an approval-gated task.
     */
    @PostMapping("/api/executions/{id}/approval")
    public ResponseEntity<Void> resolveApproval(@PathVariable String id,
                                                @RequestBody ApprovalRequest request) {
        boolean resolved = executionService.resolveApproval(id, request);
        if (resolved) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
}
