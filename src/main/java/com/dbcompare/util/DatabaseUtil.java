package com.dbcompare.util;

import com.dbcompare.config.DatabaseProperties;
import com.dbcompare.entity.DataRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.*;

@Component
public class DatabaseUtil {

    @Autowired
    private DatabaseProperties dbProperties;

    private String oracleUrl;
    private String oracleUsername;
    private String oraclePassword;
    private String postgresUrl;
    private String postgresUsername;
    private String postgresPassword;

    @PostConstruct
    private void init() {
        this.oracleUrl = dbProperties.getOracle().getUrl();
        this.oracleUsername = dbProperties.getOracle().getUsername();
        this.oraclePassword = dbProperties.getOracle().getPassword();
        this.postgresUrl = dbProperties.getPostgres().getUrl();
        this.postgresUsername = dbProperties.getPostgres().getUsername();
        this.postgresPassword = dbProperties.getPostgres().getPassword();
    }

    /**
     * 根据数据库类型获取连接
     */
    public Connection getConnection(String dbType) throws SQLException {
        switch (dbType.toUpperCase()) {
            case "ORACLE":
                return DriverManager.getConnection(
                    oracleUrl,
                    oracleUsername,
                    oraclePassword
                );
            case "POSTGRES":
                return DriverManager.getConnection(
                    postgresUrl,
                    postgresUsername,
                    postgresPassword
                );
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 查询数据库并返回DataRecord列表
     */
    public List<DataRecord> queryDatabase(String dbType, String tableName, Map<String, String> params,
                                        java.util.Date startTime, java.util.Date endTime, String timeColumn, int offset, int limit) throws SQLException {
        List<DataRecord> records = new ArrayList<>();
        
        String sql = buildQuerySQL(tableName, params, timeColumn);
        
        try (Connection conn = getConnection(dbType);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置时间参数
            stmt.setTimestamp(1, new java.sql.Timestamp(startTime.getTime()));
            stmt.setTimestamp(2, new java.sql.Timestamp(endTime.getTime()));
            
            // 设置其他参数
            int paramIndex = 3;
            for (String value : params.values()) {
                stmt.setString(paramIndex++, value);
            }
            
            // 设置分页参数
            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);
            
            // 执行查询
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                DataRecord record = mapResultSetToDataRecord(rs);
                records.add(record);
            }
        }
        
        return records;
    }

    /**
     * 构建查询SQL
     */
    private String buildQuerySQL(String tableName, Map<String, String> params, String timeColumn) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName).append(" WHERE ");
        sql.append(timeColumn).append(" BETWEEN ? AND ?");
        
        for (String paramKey : params.keySet()) {
            sql.append(" AND ").append(paramKey).append(" = ?");
        }
        
        sql.append(" ORDER BY ").append(timeColumn).append(" ASC LIMIT ? OFFSET ?");
        
        return sql.toString();
    }

    /**
     * 将ResultSet映射到DataRecord对象
     */
    private DataRecord mapResultSetToDataRecord(ResultSet rs) throws SQLException {
        DataRecord record = new DataRecord();
        Map<String, Object> fields = new HashMap<>();
        
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = rs.getObject(i);
            fields.put(columnName.toLowerCase(), value); // 转换为小写以保持一致性
        }
        
        record.setFields(fields);
        return record;
    }

    /**
     * 分页查询数据库 - 返回指定页数的数据（实现功能4：每次加载2页数据）
     */
    public List<DataRecord> queryDatabaseWithPaging(String dbType, String tableName, Map<String, String> params,
                                                  java.util.Date startTime, java.util.Date endTime, String timeColumn, int pageCount) throws SQLException {
        List<DataRecord> allRecords = new ArrayList<>();
        int offset = 0;
        int limit = 500; // 每页最多500条数据
        int pagesToLoad = pageCount; // 加载指定页数的数据

        for (int page = 0; page < pagesToLoad; page++) {
            List<DataRecord> pageRecords = queryDatabase(dbType, tableName, params, startTime, endTime, timeColumn, offset, limit);
            allRecords.addAll(pageRecords);
            
            if (pageRecords.size() < limit) {
                // 如果当前页数据不足limit条，说明已经没有更多数据了
                break;
            }
            
            offset += limit;
        }

        return allRecords;
    }
    
    /**
     * 基础分页查询（默认加载1页）
     */
    public List<DataRecord> queryDatabaseWithPaging(String dbType, String tableName, Map<String, String> params,
                                                  java.util.Date startTime, java.util.Date endTime, String timeColumn) throws SQLException {
        return queryDatabaseWithPaging(dbType, tableName, params, startTime, endTime, timeColumn, 1);
    }
    
    /**
     * 查询指定页数的数据（实现功能4：每次加载2页数据）
     */
    public List<DataRecord> queryDatabaseWithMultiplePages(String dbType, String tableName, Map<String, String> params,
                                                        java.util.Date startTime, java.util.Date endTime, String timeColumn) throws SQLException {
        // 实现功能4：每次加载2页数据，以确保跨页数据能正确匹配
        return queryDatabaseWithPaging(dbType, tableName, params, startTime, endTime, timeColumn, 2);
    }
}