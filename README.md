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



## 需求描述：
项目主要功能：DB数据对比。
项目背景： 做双出测试，系统的数据输出到两个DB表，两个表的数据结构完全一样，现在需要对比两个表数据的一致性。
技术栈：使用springboot + mybatis-plus框架来实现。
DB： 用来对比的两个数据库，一个是oracle，一个是postgres
功能概述： 
首先我们有一个配置，名称：config,json格式，属性1：params，其值是一个Map，允许用户手动输入key，value。 属性2，times。

{
   params: {'factory_id':'A','system_id':'B'}
    times:  {
			 timeCloumn:'createTime',
	         maxTime:'2026-03-24 15:00:09.000',
			 minTime: '2024-04-14 12:00:00.000'
	        }
	
   }
   'inval': 2,
   'maxCount' : 200，
   'baseDB': 'Oracle'
   ‘tableName’: 'alarm_all'，
   ‘othersClounm’: []
}

功能1,系统开放两个接口。分别用来执行和结束DB数据对比任务，任务启动时，生成一个自增id，该Id为接口回参。

功能2，执行DB任务的接口的入参就是上面的config数据，拿到该入参后，根据baseDB参数选择对应的DB连接，然后开始拼接查询SQL，我们要查询的就是tableName表，要查询的时间范围根据times里面的参数来，因为每个表时间字段不一样，有的叫createDT，有的叫createTime，具体的时间字段名称根据cloumnName的值来取，然后时间就在minTime,maxTime之内，同时使用升序排列的结果。拼接好时间参数后，再根据params里面的参数来拼接查询条件，都是用=号来拼接。

功能3，同样的道理，像功能2一样，去另外一个DB查数据。查到后，将两个list数据据开始对比，把第一个list里面的每一条去第二个list里面找，去寻找有没有除了othersClounm里的字段外完全一模一样的，且timeCloumn时间差值在前后3s内的值，如果有，就同时移除两个list里的这条数据，这样一条一条的找，直到将最后剩下的数据找出来，剩下的数据即为不匹配的数据，将其存入list3。

功能4: 再优化下查询机制，每次拿到minTime和maxTime后，将其每5分钟划分一个查询任务，然后每组任务的时间重合30s。比如输入的1点到4点，那么第一个查询任务就是1:00:00-1:05:00， 第二个查询任务是：1:05:00-1:10:00，以此类推。同时，根据list1中的数据，找list2中的数据时，请从中位数开始找，比如list1，总共200条数据，请从第100条开始找，第100条找完，找101，101找完找99。

功能5：对比出结果后，剩余的数据，就是找不到了，就部分数据有些我们要消除掉，就是 在每组任务起始和结束时间3s内的数据，这部分数据找不到是合理的，不需要记录到list3中

功能6： 最终将匹配不上的list3的数据以excel的样式，输出到当前工程的目录下。且每次往list3中放数据时，一旦list3的size大于maxCount，即刻停止当前任务，并将list3以excel文件输出。
