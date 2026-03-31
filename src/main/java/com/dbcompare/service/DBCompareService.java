package com.dbcompare.service;

import com.dbcompare.config.DBCompareConfig;
import com.dbcompare.entity.DataRecord;
import com.dbcompare.entity.TaskInfo;
import com.dbcompare.entity.TimeRangeSegment;
import com.dbcompare.util.DatabaseUtil;
import com.dbcompare.util.ExcelExportUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DBCompareService {

    // 存储当前运行的任务
    private final Map<String, TaskInfo> runningTasks = new ConcurrentHashMap<>();
    
    // 限制最大并发任务数为5
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_TASKS = 5;
    
    @Autowired
    private DatabaseUtil databaseUtil;

    /**
     * 启动数据库对比任务
     */
    public String startCompareTask(DBCompareConfig config) {
        if (activeTaskCount.get() >= MAX_CONCURRENT_TASKS) {
            throw new RuntimeException("超出最大并发任务数限制：" + MAX_CONCURRENT_TASKS);
        }

        String taskId = UUID.randomUUID().toString();
        
        // 增加活动任务计数
        activeTaskCount.incrementAndGet();
        
        TaskInfo taskInfo = new TaskInfo(taskId);
        runningTasks.put(taskId, taskInfo);

        // 在实际应用中，这里应该启动一个新的线程或使用线程池来异步执行任务
        // 这里只是模拟，实际实现需要考虑异步执行
        executeCompareTask(taskId, config);

        return taskId;
    }

    /**
     * 执行具体的对比任务
     */
    private void executeCompareTask(String taskId, DBCompareConfig config) {
        try {
            // 根据配置划分时间段
            List<TimeRangeSegment> timeSegments = createTimeSegments(config);
            
            List<DataRecord> unmatchedRecords = new ArrayList<>();
            
            // 遍历每个时间段进行对比
            for (TimeRangeSegment segment : timeSegments) {
                List<DataRecord> segmentUnmatchedRecords = compareSegment(taskId, config, segment);
                
                // 将当前段未匹配的数据添加到总列表
                unmatchedRecords.addAll(segmentUnmatchedRecords);
                
                // 检查是否超过最大数量限制
                if (unmatchedRecords.size() > config.getMaxCount()) {
                    // 立即导出Excel并停止任务
                    String filePath = exportToExcel(unmatchedRecords.subList(0, config.getMaxCount()), config.getTableName(), taskId);
                    TaskInfo taskInfo = runningTasks.get(taskId);
                    taskInfo.setStatus("COMPLETED_LIMITED");
                    taskInfo.setResultFilePath(filePath);
                    taskInfo.setEndTime(new Date());
                    activeTaskCount.decrementAndGet();
                    return;
                }
            }
            
            // 导出最终结果
            String filePath = exportToExcel(unmatchedRecords, config.getTableName(), taskId);
            TaskInfo taskInfo = runningTasks.get(taskId);
            taskInfo.setStatus("COMPLETED");
            taskInfo.setResultFilePath(filePath);
            taskInfo.setEndTime(new Date());
        } catch (Exception e) {
            TaskInfo taskInfo = runningTasks.get(taskId);
            taskInfo.setStatus("FAILED");
            taskInfo.setErrorMessage(e.getMessage());
            taskInfo.setEndTime(new Date());
            e.printStackTrace();
        } finally {
            activeTaskCount.decrementAndGet();
        }
    }

    /**
     * 创建时间分段，每段覆盖5分钟，不需要重叠
     */
    private List<TimeRangeSegment> createTimeSegments(DBCompareConfig config) {
        List<TimeRangeSegment> segments = new ArrayList<>();
        
        try {
            // 解析最小时间和最大时间
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date minTime = sdf.parse(config.getTimes().getMinTime());
            Date maxTime = sdf.parse(config.getTimes().getMaxTime());
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(minTime);
            
            Date currentStart = new Date(cal.getTimeInMillis());
            
            while (currentStart.before(maxTime)) {
                cal.setTime(currentStart);
                cal.add(Calendar.MINUTE, 5); // 每段5分钟
                Date currentEnd = new Date(cal.getTimeInMillis());
                
                // 确保不超过最大时间
                if (currentEnd.after(maxTime)) {
                    currentEnd = maxTime;
                }
                
                // 创建时间段
                segments.add(new TimeRangeSegment(
                    new Date(currentStart.getTime()),
                    new Date(currentEnd.getTime())
                ));
                
                // 计算下一个时间段的开始（不重叠）
                currentStart = currentEnd;
            }
            
            // 添加边界忽略标记
            addBoundaryIgnoreFlags(segments, minTime, maxTime, config.getInval());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return segments;
    }

    /**
     * 为时间段添加边界忽略标记
     */
    private void addBoundaryIgnoreFlags(List<TimeRangeSegment> segments, Date originalMin, Date originalMax, Integer inval) {
        long intervalMs = inval != null ? inval * 1000L : 3000L; // 默认3秒
        
        for (int i = 0; i < segments.size(); i++) {
            TimeRangeSegment segment = segments.get(i);
            
            // 检查是否接近原始最小时间
            if (Math.abs(segment.getStartTime().getTime() - originalMin.getTime()) <= intervalMs) {
                // 标记开始边界需要忽略
                segments.set(i, new TimeRangeSegment(
                    new Date(segment.getStartTime().getTime() + intervalMs),
                    segment.getEndTime(),
                    true
                ));
            } else if (Math.abs(segment.getEndTime().getTime() - originalMax.getTime()) <= intervalMs) {
                // 接近原始最大时间，标记结束边界需要忽略
                segments.set(i, new TimeRangeSegment(
                    segment.getStartTime(),
                    new Date(segment.getEndTime().getTime() - intervalMs),
                    true
                ));
            }
        }
    }

    /**
     * 对单个时间段进行数据对比
     */
    private List<DataRecord> compareSegment(String taskId, DBCompareConfig config, TimeRangeSegment segment) {
        List<DataRecord> unmatchedRecords = new ArrayList<>();
        
        try {
            // 获取基准数据库数据（作为参照），查询 5 分钟的数据
            List<DataRecord> baseDBRecords = queryDatabase(
                config.getBaseDB(), 
                config.getTableName(), 
                config.getParams(), 
                segment.getStartTime(), 
                segment.getEndTime(),
                config.getTimes().getTimeCloumn()
            );
            
            // 获取另一个数据库数据，时间区间两侧各多取 2 秒
            Date extendedStartTime = new Date(segment.getStartTime().getTime() - 2000); // 开始时间提前 2 秒
            Date extendedEndTime = new Date(segment.getEndTime().getTime() + 2000);   // 结束时间延后 2 秒
            
            String otherDB = config.getBaseDB().equals("Oracle") ? "Postgres" : "Oracle";
            List<DataRecord> otherDBRecords = queryDatabase(
                otherDB, 
                config.getTableName(), 
                config.getParams(), 
                extendedStartTime, 
                extendedEndTime,
                config.getTimes().getTimeCloumn()
            );
            
            System.out.println(String.format("\n========== 时间段对比：%s - %s ==========", 
                segment.getStartTime(), segment.getEndTime()));
            System.out.println("基准数据库记录数：" + baseDBRecords.size());
            System.out.println("待对比数据库记录数：" + otherDBRecords.size());
            
            // 进行数据对比
            List<DataRecord> remainingBaseRecords = new ArrayList<>(baseDBRecords);
            List<DataRecord> remainingOtherRecords = new ArrayList<>(otherDBRecords);
            
            // 实现数据对比逻辑
            performComparison(remainingBaseRecords, remainingOtherRecords, config.getOthersClounm(), config.getTimes().getTimeCloumn());
            
            // 收集未匹配的数据
            boolean hasUnmatched = false;
            
            // 处理基准表中未被消除的数据
            if (!remainingBaseRecords.isEmpty()) {
                hasUnmatched = true;
                System.out.println("\n【基准表未匹配数据】共 " + remainingBaseRecords.size() + " 条:");
                for (DataRecord record : remainingBaseRecords) {
                    System.out.println(record);
                }
                unmatchedRecords.addAll(remainingBaseRecords);
            }
            
            // 处理待对比表中未被消除的数据（在边界 2 秒外的）
            List<DataRecord> unmatchedOtherRecords = new ArrayList<>();
            for (DataRecord record : remainingOtherRecords) {
                Date recordTime = record.getTimestampByColumn(config.getTimes().getTimeCloumn());
                if (recordTime != null && !isInBoundary(recordTime, segment, 2)) { // 2 秒边界
                    unmatchedOtherRecords.add(record);
                }
            }
            
            if (!unmatchedOtherRecords.isEmpty()) {
                hasUnmatched = true;
                System.out.println("\n【待对比表未匹配数据 (边界 2 秒外)】共 " + unmatchedOtherRecords.size() + " 条:");
                for (DataRecord record : unmatchedOtherRecords) {
                    System.out.println(record);
                }
                unmatchedRecords.addAll(unmatchedOtherRecords);
            }
            
            // 如果所有数据都对比通过，则不打印
            if (!hasUnmatched) {
                System.out.println("✓ 该时间段内所有数据对比通过！");
            }
            
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("时间段对比失败：" + e.getMessage());
        }
        
        return unmatchedRecords;
    }

    /**
     * 检查时间是否在边界区域内
     */
    private boolean isInBoundary(Date recordTime, TimeRangeSegment segment, Integer inval) {
        long intervalMs = inval != null ? inval * 1000L : 3000L; // 默认3秒
        
        // 检查是否在开始边界内
        long startBoundaryEnd = segment.getStartTime().getTime() + intervalMs;
        if (recordTime.getTime() <= startBoundaryEnd) {
            return true;
        }
        
        // 检查是否在结束边界内
        long endBoundaryStart = segment.getEndTime().getTime() - intervalMs;
        if (recordTime.getTime() >= endBoundaryStart) {
            return true;
        }
        
        return false;
    }

    /**
     * 查询数据库
     */
    private List<DataRecord> queryDatabase(String dbType, String tableName, Map<String, String> params, 
                                         java.util.Date startTime, java.util.Date endTime, String timeColumn) {
        try {
            // 使用 DatabaseUtil 进行实际查询，不分页，一次性查询所有数据
            return databaseUtil.queryDatabaseAll(dbType, tableName, params, startTime, endTime, timeColumn);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库查询失败：" + e.getMessage());
        }
    }

    /**
     * 执行数据对比逻辑 - 从基准数据第一条开始，在待对比数据中查找时间±2 秒内的匹配项
     */
    private void performComparison(List<DataRecord> baseRecords, List<DataRecord> otherRecords, 
                                 List<String> ignoreFields, String timeColumn) {
        // 从数据库查询的数据已经是按时间升序排列的，无需再次排序
        // 从基准数据的第一条开始，逐条在待对比数据中查找
        
        int baseIndex = 0;
        while (baseIndex < baseRecords.size()) {
            DataRecord baseRecord = baseRecords.get(baseIndex);
            Date baseTime = baseRecord.getTimestampByColumn(timeColumn);
            
            if (baseTime == null) {
                baseIndex++;
                continue;
            }
            
            // 在待对比数据中查找匹配项（时间差在 2 秒内）
            int matchIndex = findClosestMatch(otherRecords, baseRecord, ignoreFields, timeColumn, baseTime);
            
            if (matchIndex != -1) {
                // 找到匹配项，从两个列表中移除
                baseRecords.remove(baseIndex);
                otherRecords.remove(matchIndex);
                // 注意：不移位，因为删除后下一个元素会自动到当前位置
            } else {
                // 未找到匹配项，继续下一条
                baseIndex++;
            }
        }
    }
    
    /**
     * 在待对比数据中查找最接近的匹配项（时间差在 2 秒内）
     */
    private int findClosestMatch(List<DataRecord> targetList, DataRecord sourceRecord, 
                                List<String> ignoreFields, String timeColumn, Date sourceTime) {
        if (targetList.isEmpty()) {
            return -1;
        }
        
        int closestIndex = -1;
        long minTimeDiff = Long.MAX_VALUE;
        
        // 遍历所有待对比数据，查找时间差在 2 秒内且字段匹配的记录
        for (int i = 0; i < targetList.size(); i++) {
            DataRecord targetRecord = targetList.get(i);
            Date targetTime = targetRecord.getTimestampByColumn(timeColumn);
            
            if (targetTime == null) continue;
            
            // 检查时间差是否在 2 秒内
            long timeDiff = Math.abs(sourceTime.getTime() - targetTime.getTime());
            
            if (timeDiff <= 2000) {
                // 时间相近，检查字段是否匹配
                if (sourceRecord.equalsIgnoreFields(targetRecord, ignoreFields)) {
                    // 如果时间差更小，更新最接近的匹配
                    if (timeDiff < minTimeDiff) {
                        minTimeDiff = timeDiff;
                        closestIndex = i;
                    }
                }
            }
        }
        
        return closestIndex; // 返回最接近的匹配索引
    }

    /**
     * 导出到Excel文件
     */
    private String exportToExcel(List<DataRecord> records, String tableName, String taskId) {
        // 使用ExcelExportUtil进行实际的Excel导出
        return ExcelExportUtil.exportToExcel(records, tableName, taskId);
    }

    /**
     * 停止指定的任务
     */
    public boolean stopCompareTask(String taskId) {
        TaskInfo taskInfo = runningTasks.get(taskId);
        if (taskInfo != null) {
            taskInfo.setStatus("STOPPED");
            taskInfo.setEndTime(new Date());
            runningTasks.remove(taskId);
            activeTaskCount.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * 获取任务状态
     */
    public TaskInfo getTaskStatus(String taskId) {
        return runningTasks.get(taskId);
    }

    /**
     * 获取当前活跃任务数量
     */
    public int getActiveTaskCount() {
        return activeTaskCount.get();
    }
}