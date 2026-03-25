package com.dbcompare.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DataRecord {
    private String id;
    private Map<String, Object> fields = new HashMap<>();
    private Date timestamp;

    public DataRecord() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public DataRecord(String id) {
        this.id = id;
    }

    // 根据指定的时间列获取时间戳
    public Date getTimestampByColumn(String timeColumnName) {
        Object timeValue = fields.get(timeColumnName);
        if (timeValue instanceof Date) {
            return (Date) timeValue;
        } else if (timeValue instanceof String) {
            // 如果是字符串格式的时间，需要解析
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse((String) timeValue);
            } catch (Exception e) {
                try {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) timeValue);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
        } else if (timeValue instanceof java.sql.Timestamp) {
            return new Date(((java.sql.Timestamp) timeValue).getTime());
        }
        return null;
    }

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DataRecord that = (DataRecord) obj;
        
        // 比较所有字段，除了被忽略的字段
        if (this.fields.size() != that.fields.size()) {
            return false;
        }
        
        for (String key : this.fields.keySet()) {
            if (!that.fields.containsKey(key)) {
                return false;
            }
            
            Object thisValue = this.fields.get(key);
            Object thatValue = that.fields.get(key);
            
            if (thisValue == null ? thatValue != null : !thisValue.equals(thatValue)) {
                return false;
            }
        }
        
        return true;
    }

    // 忽略特定字段的比较方法
    public boolean equalsIgnoreFields(DataRecord other, java.util.List<String> ignoreFields) {
        if (this == other) return true;
        if (other == null) return false;
        
        for (String key : this.fields.keySet()) {
            // 跳过忽略的字段
            if (ignoreFields != null && ignoreFields.contains(key)) {
                continue;
            }
            
            if (!other.fields.containsKey(key)) {
                return false;
            }
            
            Object thisValue = this.fields.get(key);
            Object otherValue = other.fields.get(key);
            
            if (thisValue == null ? otherValue != null : !thisValue.equals(otherValue)) {
                return false;
            }
        }
        
        // 检查other是否有多余的非忽略字段
        for (String key : other.fields.keySet()) {
            if (ignoreFields != null && ignoreFields.contains(key)) {
                continue;
            }
            
            if (!this.fields.containsKey(key)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DataRecord{" +
                "id='" + id + '\'' +
                ", fields=" + fields +
                ", timestamp=" + timestamp +
                '}';
    }
}