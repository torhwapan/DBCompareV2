# DBCompareV2 - 数据库对比工具

## 项目概述
这是一个基于Spring Boot + MyBatis-Plus的数据库对比工具，用于执行双出测试中两个相同结构数据库表的数据一致性对比。

## 功能特性

### 主要功能
1. **并发任务管理**：支持最多5个并发任务，每个任务有唯一ID
2. **灵活配置**：支持通过JSON配置参数、时间范围等
3. **智能分段**：自动将时间范围按5分钟划分，每段重合30秒
4. **高效对比**：使用中位数搜索算法提高数据对比效率
5. **边界处理**：自动忽略时间边界3秒内的异常数据
6. **分页查询**：支持大数据量的分页查询
7. **Excel导出**：将不匹配数据导出为Excel文件

### 技术栈
- Spring Boot 2.7.0
- MyBatis-Plus 3.5.2
- Oracle JDBC Driver
- PostgreSQL JDBC Driver
- Apache POI (Excel导出)

## 核心类说明

### 配置类
- [DBCompareConfig](./src/main/java/com/dbcompare/config/DBCompareConfig.java)：数据库对比配置类，对应用户提供的JSON配置
- [DatabaseProperties](./src/main/java/com/dbcompare/config/DatabaseProperties.java)：数据库连接配置

### 实体类
- [TaskInfo](./src/main/java/com/dbcompare/entity/TaskInfo.java)：任务信息实体
- [DataRecord](./src/main/java/com/dbcompare/entity/DataRecord.java)：数据记录实体
- [TimeRangeSegment](./src/main/java/com/dbcompare/entity/TimeRangeSegment.java)：时间范围分段实体

### 服务类
- [DBCompareService](./src/main/java/com/dbcompare/service/DBCompareService.java)：核心业务逻辑服务
- [DatabaseUtil](./src/main/java/com/dbcompare/util/DatabaseUtil.java)：数据库操作工具类
- [ExcelExportUtil](./src/main/java/com/dbcompare/util/ExcelExportUtil.java)：Excel导出工具类

### 控制器类
- [DBCompareController](./src/main/java/com/dbcompare/controller/DBCompareController.java)：提供REST API接口

## API接口

### 启动对比任务
```
POST /api/db-compare/start
Content-Type: application/json

{
  "params": {"factory_id":"A","system_id":"B"},
  "times": {
    "timeCloumn":"createTime",
    "maxTime":"2026-03-24 15:00:09.000",
    "minTime": "2024-04-14 12:00:00.000"
  },
  "inval": 2,
  "maxCount": 200,
  "baseDB": "Oracle",
  "tableName": "alarm_all",
  "othersClounm": []
}
```

### 停止对比任务
```
POST /api/db-compare/stop/{taskId}
```

### 获取任务状态
```
GET /api/db-compare/status/{taskId}
```

### 获取活跃任务数
```
GET /api/db-compare/active-count
```

## 配置文件
- [application.yml](./src/main/resources/application.yml)：应用配置文件
- [pom.xml](./pom.xml)：Maven依赖配置

## 使用说明

1. 修改 [application.yml](./src/main/resources/application.yml) 中的数据库连接配置
2. 启动Spring Boot应用
3. 通过API接口启动、停止对比任务
4. 查看控制台输出和生成的Excel文件

## 核心算法说明

1. **时间分段算法**：将时间范围按5分钟切分，相邻段重合30秒
2. **数据对比算法**：使用中位数搜索法，从中间位置开始匹配，提高查找效率
3. **分页查询机制**：每次加载2页数据，避免跨页数据匹配问题
4. **边界数据处理**：忽略时间边界3秒内的数据，减少异常影响

## 注意事项

- 确保Oracle和PostgreSQL驱动正确配置
- 数据库连接信息需在配置文件中正确设置
- 任务最大并发数限制为5个
- 不匹配数据超过200条时会立即导出Excel并停止任务