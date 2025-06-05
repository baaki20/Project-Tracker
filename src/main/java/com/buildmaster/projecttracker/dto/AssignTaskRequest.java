package com.buildmaster.projecttracker.dto;

import lombok.Data;

@Data
public class AssignTaskRequest {
    private Long taskId;
    private Long developerId;
}