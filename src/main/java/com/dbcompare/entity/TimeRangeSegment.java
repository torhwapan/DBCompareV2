package com.dbcompare.entity;

import java.util.Date;

public class TimeRangeSegment {
    private Date startTime;
    private Date endTime;
    private boolean isBoundaryIgnored; // 是否忽略边界数据（在边界3秒内的数据）

    public TimeRangeSegment(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBoundaryIgnored = false;
    }

    public TimeRangeSegment(Date startTime, Date endTime, boolean isBoundaryIgnored) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBoundaryIgnored = isBoundaryIgnored;
    }

    // getters and setters
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

    public boolean isBoundaryIgnored() {
        return isBoundaryIgnored;
    }

    public void setBoundaryIgnored(boolean boundaryIgnored) {
        isBoundaryIgnored = boundaryIgnored;
    }

    @Override
    public String toString() {
        return "TimeRangeSegment{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", isBoundaryIgnored=" + isBoundaryIgnored +
                '}';
    }
}