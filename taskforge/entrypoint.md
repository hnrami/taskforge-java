I am building TaskForge Engineering Assignment.

First read these project documents:

- TASKFORGE_IMPLEMENTATION_PLAN.md
- ARCHITECTURE.md
- DECISIONS.md
- TESTDESIGN.md

Understand the complete architecture before generating code.

Technology stack:
- java
- Spring Boot
- maven
- Spring Web
- Spring Data JPA
- H2 Database
- JUnit 5

IMPORTANT DESIGN RULES:

1. ExecutionEngine must never know concrete task types.
2. Do not use if/else or switch based on task type.
3. Follow Open/Closed principle.
4. All tasks must execute through TaskHandler interface.
5. New task types should be added without modifying engine code.

TASK 1:

Create only the initial project foundation:

- maven configuration
- Package structure
- Domain models
- Enums
- Interfaces

Create packages:

com.taskforge

api
service
engine
handler
model
repository
exception


Create models:

WorkflowDefinition
TaskDefinition
WorkflowExecution
TaskExecution
RetryPolicy
TaskContext
TaskResult


Create enums:

ExecutionStatus
TaskStatus


Create handler contract:

TaskHandler interface


Create empty:

TaskHandlerRegistry


Do NOT implement:

- REST APIs
- DAG validation
- Execution engine
- Task handlers
- Database logic

Only create clean foundation code first.

After generating code explain the created structure.