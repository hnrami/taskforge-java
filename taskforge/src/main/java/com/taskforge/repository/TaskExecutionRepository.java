package com.taskforge.repository;

import com.taskforge.model.TaskExecutionEntity;
import com.taskforge.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, String> {

    List<TaskExecutionEntity> findByWorkflowExecutionId(String workflowExecutionId);

    List<TaskExecutionEntity> findByWorkflowExecutionIdAndStatus(String workflowExecutionId, TaskStatus status);
}
