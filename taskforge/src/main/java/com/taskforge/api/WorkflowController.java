package com.taskforge.api;

import com.taskforge.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * POST /api/workflows
     * Validates DAG and registers a new workflow definition.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflow(@RequestBody CreateWorkflowRequest request) {
        return workflowService.createWorkflow(request);
    }

    /**
     * GET /api/workflows/{id}
     * Returns the workflow definition by ID.
     */
    @GetMapping("/{id}")
    public WorkflowResponse getWorkflow(@PathVariable String id) {
        return workflowService.getWorkflow(id);
    }
}

