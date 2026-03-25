package com.dbcompare.config;

import java.util.List;
import java.util.Map;

public class DBCompareConfig {
    private Map<String, String> params;
    private TimeRange times;
    private Integer inval;
    private Integer maxCount;
    private String baseDB;
    private String tableName;
    private List<String> othersClounm;

    // 时间范围内部类
    public static class TimeRange {
        private String timeCloumn;
        private String maxTime;
        private String minTime;

        // getters and setters
        public String getTimeCloumn() {
            return timeCloumn;
        }

        public void setTimeCloumn(String timeCloumn) {
            this.timeCloumn = timeCloumn;
        }

        public String getMaxTime() {
            return maxTime;
        }

        public void setMaxTime(String maxTime) {
            this.maxTime = maxTime;
        }

        public String getMinTime() {
            return minTime;
        }

        public void setMinTime(String minTime) {
            this.minTime = minTime;
        }
    }

    // getters and setters
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public TimeRange getTimes() {
        return times;
    }

    public void setTimes(TimeRange times) {
        this.times = times;
    }

    public Integer getInval() {
        return inval;
    }

    public void setInval(Integer inval) {
        this.inval = inval;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public String getBaseDB() {
        return baseDB;
    }

    public void setBaseDB(String baseDB) {
        this.baseDB = baseDB;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getOthersClounm() {
        return othersClounm;
    }

    public void setOthersClounm(List<String> othersClounm) {
        this.othersClounm = othersClounm;
    }
}