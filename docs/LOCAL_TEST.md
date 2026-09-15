# 本地运行与验收说明

本文用于验收 Profile-only V1，建议按顺序执行：**环境检查 -> Mock 回归 -> 核心无依赖回归 -> 打包 -> 达梦实库 -> Excel/HTML 人工验收**。

## 一、环境要求

建议环境：

- JDK 8
- Maven 3.x
- Git Bash（Windows）
- 达梦 JDBC Driver（实库测试时需要）

先检查：

```bash
java -version
javac -version
mvn -version
```

重点确认 Maven 实际使用的 Java 版本。如果本机安装了多个 JDK，`mvn -version` 中的 Java home 应该指向准备验收的 JDK。

## 二、获取正式代码

本轮 V1 合并以后，以 `main` 为准：

```bash
git clone https://github.com/Initial-neko/quality_analysis.git
cd quality_analysis
git checkout main
git pull
```

如果仓库已经存在：

```bash
git fetch origin
git checkout main
git pull origin main
```

## 三、第一层：完整 Maven 回归

执行：

```bash
mvn clean test
```

期望：

```text
BUILD SUCCESS
```

这一层会验证：

- Profile 核心指标
- Minimal Profile 默认开关
- JDBC 扫描 SQL 约束
- Mock CUSTOMER / ORDERS
- 每表 JSON 持久化和重新读取
- manifest 保存
- Excel 生成和关键 Sheet
- HTML 生成和关键内容

如果这里失败，先不要接真实达梦。

## 四、第二层：只验证 Profile 核心

如果希望确认 Profile 核心不依赖 Gson / POI，可以在 Git Bash 中执行：

```bash
bash scripts/manual-regression.sh
```

这个脚本只编译和运行 Profile 核心，不编译 persistence/report/run/cli 层。

期望看到类似：

```text
Manual regression checks passed
```

这个测试的意义是：即使以后报告依赖发生变化，核心 Profile 仍然可以单独验证。

## 五、打包

执行：

```bash
mvn clean package
```

生成：

```text
target/quality-analysis-0.1.0-SNAPSHOT.jar
```

由于当前不是 fat-jar，Gson 和 Apache POI 仍然是独立运行依赖。把 runtime 依赖复制到：

```bash
mvn dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/dependency
```

此时应有：

```text
target/
├── quality-analysis-0.1.0-SNAPSHOT.jar
└── dependency/
    ├── gson-2.10.1.jar
    ├── poi-5.2.2.jar
    ├── poi-ooxml-5.2.2.jar
    └── POI 的其他传递依赖...
```

## 六、准备达梦 JDBC Driver

达梦 JDBC jar 不提交仓库。

例如本地：

```text
D:\dm\DmJdbcDriver18.jar
```

实际使用你们环境中已经验证可以连接达梦的 JDBC jar 即可。

## 七、先做单表控制台测试

建议不要一开始就扫描很多表，先验证一张较小的业务表。

参数格式：

```text
<driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]
```

Git Bash 示例：

```bash
java -cp "target/quality-analysis-0.1.0-SNAPSHOT.jar;target/dependency/*;D:/dm/DmJdbcDriver18.jar" \
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli \
  dm.jdbc.driver.DmDriver \
  "jdbc:dm://127.0.0.1:5236/DAMENG" \
  USER PASSWORD TEST CUSTOMER 10000
```

重点看：

- 是否成功连接
- 表名 / Schema 是否正确
- rowCount 是否与预期一致
- NULL / Distinct / Min / Max 是否明显合理
- CLOB/BLOB 是否没有导致内存异常

单表模式只打印控制台结果，不生成 Excel/HTML。

## 八、再做正式报告模式测试

参数格式：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <schema>
<table1,table2,...> <output-root> [fetchSize]
```

示例：

```bash
java -cp "target/quality-analysis-0.1.0-SNAPSHOT.jar;target/dependency/*;D:/dm/DmJdbcDriver18.jar" \
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli \
  dm.jdbc.driver.DmDriver \
  "jdbc:dm://127.0.0.1:5236/DAMENG" \
  USER PASSWORD DM_TEST TEST \
  "CUSTOMER,ORDERS" \
  "D:/quality-runs" \
  10000
```

其中：

```text
database-label = 报告里展示的数据库标识，不是密码，也不参与 JDBC 连接
table1,table2  = 同一个 Schema 下需要扫描的表
output-root    = 所有 run 目录的根路径
fetchSize      = 默认 10000，不传也可以
```

执行完成后控制台会打印本次 run 目录。

## 九、检查任务目录

例如：

```text
D:/quality-runs/20260915_xxx/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   └── TEST.ORDERS.json
├── quality-profile.xlsx
└── quality-profile.html
```

首先看 `manifest.json`：

- status 是否为 `COMPLETED`
- plannedTables 是否正确
- successTables 是否正确
- failedTables 是否为 0
- 每张表是否有 execution 记录

然后看 `tables/*.json`：

- 一张表一个文件
- rowCount 正确
- columns 数量正确
- 字段关键 Profile 指标合理

## 十、Excel 人工验收

打开：

```text
quality-profile.xlsx
```

### 扫描概览

确认：

- 任务信息正确
- 表数正确
- 字段总数合理
- 累计扫描行数合理
- 潜在枚举 / 候选唯一键 / 常量字段数量可以解释

### 字段质量明细

建议抽查 5～10 个已知字段：

- 表名 / 字段名
- 类型
- 是否主键
- rowCount
- NULL 数和 NULL 率
- Distinct
- 唯一率
- Min / Max
- 长度
- 枚举 / TopN

特别选择你已经知道实际数据特征的字段来比对。

### 探查提示

检查：

- 状态码/性别等字段是否可能被识别为潜在枚举
- 主键类字段是否可能识别为候选唯一键
- 来源系统等固定值字段是否识别为常量

注意：这里是 Profile Insight，不是质量异常。

## 十一、HTML 人工验收

直接双击：

```text
quality-profile.html
```

确认：

- 不需要启动任何服务即可打开
- 总体概览正确
- 表目录可以搜索
- 点击/展开表后字段信息完整
- Excel 和 HTML 的同一个字段指标一致

特别检查：

> Excel 和 HTML 都来自同一个持久化 JSON，因此同一个字段的 rowCount、Distinct、NULL 等数据必须一致。

## 十二、失败恢复测试（推荐）

可以故意在表列表中加入一个不存在的表，例如：

```text
CUSTOMER,NOT_EXISTS,ORDERS
```

期望：

- CUSTOMER 成功后立即有 JSON
- NOT_EXISTS 被记录为失败
- 程序继续尝试 ORDERS
- 前面成功表的 JSON 不丢失
- `manifest.json` 反映成功/失败数量
- 如果至少有成功表，仍可以基于成功记录生成报告

这可以验证“一张表做完立即保存”的核心设计是否真正有效。

## 十三、建议第一轮实库验收规模

不要第一次就全库扫描。建议：

```text
第 1 次：1 张小表
第 2 次：2～3 张典型表
第 3 次：包含 VARCHAR / NUMBER / DATE / CLOB / BLOB 的表
第 4 次：20 万行左右真实表
第 5 次：再逐步扩大到接近 100 万行
```

每一轮重点观察：

- 执行时间
- Java 进程内存
- 达梦数据库负载
- JSON 大小
- Excel/HTML 打开速度

如果真实表出现明显内存压力，再根据证据调整 Distinct 策略；V1 不提前引入 HLL。

## 十四、常见问题

### 1. `ClassNotFoundException: dm.jdbc.driver.DmDriver`

达梦 JDBC jar 没有加入 classpath，检查：

```text
D:/dm/DmJdbcDriver18.jar
```

路径是否正确。

### 2. 找不到 Gson / POI 类

先执行：

```bash
mvn dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/dependency
```

并确认 classpath 中包含：

```text
target/dependency/*
```

### 3. 表不存在

重点检查：

- schema 是否正确
- table 是否正确
- 当前用户是否有 SELECT 权限
- 达梦对象名大小写/引号规则是否和实际对象一致

当前 `SqlTableName` 使用安全的普通标识符策略；如果现场大量使用带引号或中文对象名，需要单独扩展标识符引用方案。

### 4. CLOB 很大

默认只读取 CLOB 长度，不读取正文；不要为了第一轮验收开启 CLOB Preview。

## 十五、验收通过标准

第一版可以认为通过，至少满足：

```text
[ ] mvn clean test 成功
[ ] manual-regression 成功
[ ] 单表达梦扫描成功
[ ] 多表报告模式成功
[ ] 每张完成表立即生成 JSON
[ ] manifest 记录正确
[ ] Excel 三个 Sheet 正常
[ ] HTML 双击可直接查看
[ ] Excel/HTML 同字段指标一致
[ ] CLOB/BLOB 没有出现明显内存问题
[ ] 不出现 Rule / PASS / FAIL / 质量分
```
