package com.taskforge.repository;

import com.taskforge.model.WorkflowExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecutionEntity, String> {

    List<WorkflowExecutionEntity> findByWorkflowDefinitionId(String workflowDefinitionId);
}
