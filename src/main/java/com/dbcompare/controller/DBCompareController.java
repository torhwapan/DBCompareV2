package com.dbcompare.controller;

import com.dbcompare.config.DBCompareConfig;
import com.dbcompare.entity.TaskInfo;
import com.dbcompare.service.DBCompareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/db-compare")
public class DBCompareController {

    @Autowired
    private DBCompareService dbCompareService;

    /**
     * 启动数据库对比任务
     */
    @PostMapping("/start")
    public String startCompareTask(@RequestBody DBCompareConfig config) {
        try {
            String taskId = dbCompareService.startCompareTask(config);
            return "{\"success\": true, \"taskId\": \"" + taskId + "\", \"message\": \"任务已启动\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 停止数据库对比任务
     */
    @PostMapping("/stop/{taskId}")
    public String stopCompareTask(@PathVariable String taskId) {
        boolean result = dbCompareService.stopCompareTask(taskId);
        if (result) {
            return "{\"success\": true, \"message\": \"任务已停止\"}";
        } else {
            return "{\"success\": false, \"message\": \"任务不存在或已停止\"}";
        }
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public String getTaskStatus(@PathVariable String taskId) {
        TaskInfo taskInfo = dbCompareService.getTaskStatus(taskId);
        if (taskInfo != null) {
            return String.format(
                "{\"taskId\": \"%s\", \"status\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"resultFilePath\": \"%s\", \"errorMessage\": \"%s\"}",
                taskInfo.getTaskId(),
                taskInfo.getStatus(),
                taskInfo.getStartTime(),
                taskInfo.getEndTime(),
                taskInfo.getResultFilePath(),
                taskInfo.getErrorMessage()
            );
        } else {
            return "{\"success\": false, \"message\": \"任务不存在\"}";
        }
    }

    /**
     * 获取当前活跃任务数量
     */
    @GetMapping("/active-count")
    public String getActiveTaskCount() {
        int count = dbCompareService.getActiveTaskCount();
        return "{\"activeTaskCount\": " + count + "}";
    }
}