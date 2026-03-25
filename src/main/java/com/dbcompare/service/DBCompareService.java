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
     * 创建时间分段，每段覆盖5分钟，重叠30秒
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
                
                // 计算下一个时间段的开始（重叠30秒）
                cal.setTime(currentStart);
                cal.add(Calendar.SECOND, 270); // 5分钟 - 30秒重叠
                currentStart = new Date(cal.getTimeInMillis());
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
            // 获取基础数据库数据
            List<DataRecord> baseDBRecords = queryDatabase(
                config.getBaseDB(), 
                config.getTableName(), 
                config.getParams(), 
                segment.getStartTime(), 
                segment.getEndTime(),
                config.getTimes().getTimeCloumn()
            );
            
            // 获取另一个数据库数据
            String otherDB = config.getBaseDB().equals("Oracle") ? "Postgres" : "Oracle";
            List<DataRecord> otherDBRecords = queryDatabase(
                otherDB, 
                config.getTableName(), 
                config.getParams(), 
                segment.getStartTime(), 
                segment.getEndTime(),
                config.getTimes().getTimeCloumn()
            );
            
            // 进行数据对比
            List<DataRecord> remainingBaseRecords = new ArrayList<>(baseDBRecords);
            List<DataRecord> remainingOtherRecords = new ArrayList<>(otherDBRecords);
            
            // 实现数据对比逻辑
            performComparison(remainingBaseRecords, remainingOtherRecords, config.getOthersClounm(), config.getTimes().getTimeCloumn());
            
            // 根据边界忽略规则过滤结果
            if (!segment.isBoundaryIgnored()) {
                unmatchedRecords.addAll(remainingBaseRecords);
                unmatchedRecords.addAll(remainingOtherRecords);
            } else {
                // 只保留不在边界区域的数据
                for (DataRecord record : remainingBaseRecords) {
                    Date recordTime = record.getTimestampByColumn(config.getTimes().getTimeCloumn());
                    if (recordTime != null && !isInBoundary(recordTime, segment, config.getInval())) {
                        unmatchedRecords.add(record);
                    }
                }
                
                for (DataRecord record : remainingOtherRecords) {
                    Date recordTime = record.getTimestampByColumn(config.getTimes().getTimeCloumn());
                    if (recordTime != null && !isInBoundary(recordTime, segment, config.getInval())) {
                        unmatchedRecords.add(record);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("时间段对比失败: " + e.getMessage());
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
            // 使用DatabaseUtil进行实际查询，实现功能4：每次加载2页数据
            return databaseUtil.queryDatabaseWithMultiplePages(dbType, tableName, params, startTime, endTime, timeColumn);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("数据库查询失败: " + e.getMessage());
        }
    }

    /**
     * 执行数据对比逻辑
     */
    private void performComparison(List<DataRecord> baseRecords, List<DataRecord> otherRecords, 
                                 List<String> ignoreFields, String timeColumn) {
        // 从数据库查询的数据已经是按时间升序排列的，无需再次排序
        // 实现功能4：从中位数开始查找
        List<Integer> checkedIndices = new ArrayList<>();
        
        // 从baseRecords中逐个取出记录，在otherRecords中查找匹配项
        for (int i = 0; i < baseRecords.size(); i++) {
            DataRecord baseRecord = baseRecords.get(i);
            Date baseTime = baseRecord.getTimestampByColumn(timeColumn);
            
            if (baseTime == null) continue;
            
            // 从中位数开始查找otherRecords
            int matchIndex = findMatchWithMedianSearch(otherRecords, baseRecord, ignoreFields, timeColumn, baseTime);
            
            if (matchIndex != -1) {
                // 找到匹配项，从两个列表中移除
                baseRecords.remove(i);
                otherRecords.remove(matchIndex);
                i--; // 因为移除了元素，需要调整索引
            }
        }
    }
    
    /**
     * 使用中位数搜索方法在目标列表中查找匹配项
     */
    private int findMatchWithMedianSearch(List<DataRecord> targetList, DataRecord sourceRecord, 
                                       List<String> ignoreFields, String timeColumn, Date sourceTime) {
        if (targetList.isEmpty()) {
            return -1;
        }
        
        int size = targetList.size();
        List<Integer> searchOrder = new ArrayList<>();
        
        // 生成搜索顺序：从中位数开始，交替向两边扩展
        int median = size / 2;
        searchOrder.add(median);
        
        int left = median - 1;
        int right = median + 1;
        
        while (left >= 0 || right < size) {
            if (left >= 0) {
                searchOrder.add(left);
                left--;
            }
            if (right < size) {
                searchOrder.add(right);
                right++;
            }
        }
        
        // 按照生成的顺序进行搜索
        for (int index : searchOrder) {
            DataRecord targetRecord = targetList.get(index);
            Date targetTime = targetRecord.getTimestampByColumn(timeColumn);
            
            if (targetTime == null) continue;
            
            // 检查时间差是否在3秒内
            long timeDiff = Math.abs(sourceTime.getTime() - targetTime.getTime());
            
            if (timeDiff <= 3000) {
                // 时间相近，检查字段是否匹配
                if (sourceRecord.equalsIgnoreFields(targetRecord, ignoreFields)) {
                    return index; // 返回匹配的索引
                }
            }
        }
        
        return -1; // 未找到匹配项
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