package com.buildmaster.projecttracker.mapper;

import com.buildmaster.projecttracker.dto.AssignTaskRequest;
import com.buildmaster.projecttracker.entity.TaskAssignment;

public class AssignTaskRequestMapper {
    public static <TaskAssignment> com.buildmaster.projecttracker.mapper.TaskAssignment toEntity(AssignTaskRequest request) {
        if (request == null) return null;
        TaskAssignment assignment = new TaskAssignment();
        assignment.getClass(request.getTaskId());
        assignment.setDeveloperId(request.getDeveloperId());
        return assignment;
    }
}
