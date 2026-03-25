package com.dbcompare.entity;

import java.util.Date;

public class TaskInfo {
    private String taskId;
    private String status; // RUNNING, COMPLETED, FAILED
    private Date startTime;
    private Date endTime;
    private String resultFilePath;
    private String errorMessage;

    public TaskInfo(String taskId) {
        this.taskId = taskId;
        this.status = "RUNNING";
        this.startTime = new Date();
    }

    // getters and setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }

    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}