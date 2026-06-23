# TaskForge Workflow Execution Flow

This diagram shows the complete state transitions and decision points during a workflow execution.

```mermaid
graph TD
    A["1. CREATE WORKFLOW<br/>POST /api/workflows<br/>localhost:8090"] -->|Validate DAG| B["WorkflowDefinition<br/>stored"]
    
    B -->|Get Workflow ID| C["2. START EXECUTION<br/>POST /api/workflows/<id>/executions<br/>localhost:8090"]
    
    C -->|Create & Start| D["WorkflowExecution<br/>status: CREATED"]
    
    D -->|Engine Starts| E["status: RUNNING"]
    
    E -->|Task Scheduler| F["Find ready tasks<br/>all deps = SUCCESS"]
    
    F -->|Run in Parallel| G["Task 1: RUNNING<br/>Task 2: RUNNING"]
    
    G -->|Complete| H["Task 1: SUCCESS<br/>Task 2: SUCCESS"]
    
    H -->|Next Ready| I{"Is there an<br/>approval task?"}
    
    I -->|Yes| J["Task 3: WAITING_APPROVAL<br/>Engine pauses"]
    
    I -->|No| K["Continue to next tasks"]
    
    J -->|3. APPROVE<br/>POST /api/executions/<id>/approval| L{"Approved?"}
    
    L -->|Yes| M["Task 3: SUCCESS<br/>Continue workflow"]
    
    L -->|No| N["Task 3: FAILED<br/>Downstream: SKIPPED"]
    
    J -->|4. CANCEL<br/>POST /api/executions/<id>/cancel| O["All tasks: CANCELLED<br/>Workflow: CANCELLED"]
    
    K -->|Continue| P{"All tasks<br/>terminal?"}
    
    P -->|Yes| Q["5. CHECK STATUS<br/>GET /api/executions/<id>"]
    
    Q -->|View Final State| R["Workflow: SUCCESS<br/>or FAILED<br/>or CANCELLED"]
    
    M -->|Continue| K
    
    style A fill:#e1f5ff
    style C fill:#e1f5ff
    style J fill:#fff3e0
    style L fill:#fff3e0
    style O fill:#ffebee
    style Q fill:#e8f5e9
    style R fill:#c8e6c9
```

## Key Points

- **Blue boxes** = API calls you make
- **Orange boxes** = Approval decision points
- **Red boxes** = Cancellation flow
- **Green boxes** = Final status checks

## Example Scenario

### Successful Flow
```
build (SUCCESS)
  ↓
test (SUCCESS)
  ↓
approval-gate → [you approve] → SUCCESS
  ↓
deploy (SUCCESS)
  ↓
Workflow: SUCCESS
```

### Rejection Flow
```
build (SUCCESS)
  ↓
test (SUCCESS)
  ↓
approval-gate → [you reject] → FAILED
  ↓
deploy (SKIPPED - never runs)
  ↓
Workflow: FAILED
```

### Cancellation Flow
```
build (SUCCESS)
  ↓
test (RUNNING)
  ↓
[you cancel]
  ↓
test (CANCELLED)
approval-gate (CANCELLED)
deploy (CANCELLED)
  ↓
Workflow: CANCELLED
```
