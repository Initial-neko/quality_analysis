# quality_analysis

一个轻量级的 Java 8 JDBC 数据探查工具。当前 V1 只做 **Profile（数据画像/探查）**：按明确提供的表清单逐表扫描，每完成一张表立即持久化，最后从持久化结果生成 Excel 和 HTML 报告。

当前首先适配达梦（DM），核心扫描逻辑基于标准 JDBC，后续可以继续适配 Oracle、MySQL、PostgreSQL 等数据库。

## 一、V1 范围

V1 **不包含字段级 Rule 规则引擎**。Rule 需要根据具体字段配置业务规则，例如手机号正则、年龄范围、字典值等；当前第一版没有这个需求，因此只保留 Profile。

之前完整的 Profile + Rule 实现保留在：

```text
archive/profile-with-rules
```

Profile 只描述“数据实际是什么样”，不擅自判断业务正确性。

## 二、正式扫描方式：显式表清单

第一版不会自动发现、枚举或扫描数据库中的全部表。正式运行时建议提供 UTF-8 表清单文件：

```text
# 默认 Schema 为 TEST
CUSTOMER
ORDERS

# 也可以显式指定 Schema
SALES.SALES_ORDER
MASTER.PRODUCT
```

规则：

- 每行一张表；
- 空行忽略；
- 以 `#` 开头的整行注释忽略；
- 重复表自动去重并保持第一次出现的顺序；
- 只写 `TABLE` 时使用命令行默认 Schema；
- 写成 `SCHEMA.TABLE` 时使用该行显式 Schema；
- 默认 Schema 传 `-` 时，文件中每一行都必须写 `SCHEMA.TABLE`；
- **程序不会查询数据库所有表后再过滤，因此不会扫描清单之外的表。**

详细说明见：

```text
docs/TABLE_LIST.md
```

发行包自带：

```text
config/tables.txt.example
```

## 三、整体处理链路

```text
UTF-8 表清单文件
      |
      v
TableListFileReader
      |
      v
TableRef 列表
      |
      v
DM / JDBC 数据库
      |
      | SELECT *
      | TYPE_FORWARD_ONLY + CONCUR_READ_ONLY
      | Statement.setFetchSize(...)
      v
JdbcTableProfiler
      |
      v
ProfileEngine
      |
      v
TableProfile
      |
      | 一张表完成后立即保存
      v
runs/<runId>/tables/<schema>.<table>.json
      |
      +--> quality-profile.xlsx
      +--> quality-profile.html
      `--> quality-profile-tables/<schema>.<table>.html
```

V1 不做应用层分页，不使用 OFFSET/LIMIT，也不要求按照主键排序。JDBC/达梦驱动负责结果集预取，程序使用 `ResultSet.next()` 顺序消费数据。

## 四、默认 Profile 指标

默认只打开高价值画像能力：

- 总行数；
- 物理 NULL 数和 NULL 率；
- 空字符串数量；
- 语义空值数量（默认识别 `NULL`、`N/A`、`NA`）；
- 精确 Distinct 数；
- 唯一率；
- 低基数值及频次；
- `distinct <= 20` 时输出全部枚举值、数量和占比；
- 数值/日期 Min、Max；
- 字符串/LOB 长度统计；
- 数据库声明主键标签；
- 候选唯一键、常量字段、准常量字段、潜在枚举字段提示。

物理 `NULL`、空串、语义空值都会保留各自计数，但不会继续进入 Distinct、唯一率、TopN、Min/Max、字符串长度、Pattern 等后续画像。

Pattern、字符组成、大小写变体、Set Fingerprint / MinHash、CLOB Preview 等能力默认关闭。

## 五、CLOB / BLOB

| 类型 | V1 行为 |
|---|---|
| CLOB / NCLOB | 默认只读取 `Clob.length()`；正文不读取；可选有限前缀预览 |
| BLOB | 只读取 `Blob.length()`；不加载字节内容 |
| VARCHAR / CHAR | 正常 Profile |

LOB 内容不参与 Distinct / 唯一率计算。

对于达梦，建议保持 `clobAsString=false`。达梦 JDBC 驱动由使用者自行提供，不提交到仓库。

## 六、结果持久化与报告

每一次扫描任务都是一个独立目录：

```text
runs/20260915_001/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   └── TEST.ORDERS.json
├── quality-profile.xlsx
├── quality-profile.html
└── quality-profile-tables/
    ├── TEST.CUSTOMER.html
    └── TEST.ORDERS.html
```

- 一张表完成后立即保存一个 JSON；
- JSON 先写临时文件，再替换正式文件；
- 中途某表失败不会丢失前面成功表；
- Excel/HTML 只读取持久化 JSON，不重新扫描数据库；
- 不使用 SQLite，当前按表落盘更适合本地、可携带的 V1。

Excel 包含 `扫描概览`、`字段质量明细`、`探查提示` 三个 Sheet。

HTML 仍然是纯静态页面，但不再把所有字段明细塞进一个文件：`quality-profile.html` 只负责表级预览和链接，每张表的字段详情位于 `quality-profile-tables/` 下。直接双击首页即可使用，不需要 Spring/Tomcat/Nginx/Node。

## 七、一条命令打包

要求：JDK 8+、Maven 3.x。

执行：

```bash
mvn clean package
```

同时运行测试并生成完整发行目录和 ZIP：

```text
target/quality-analysis-0.1.0-SNAPSHOT-distribution/
├── quality-analysis.jar
├── lib/                    # 全部 runtime 依赖
├── src/                    # main/test/assembly 源码
├── config/
│   └── tables.txt.example
├── docs/
├── scripts/
├── pom.xml
└── README.md

target/quality-analysis-0.1.0-SNAPSHOT-distribution.zip
```

不是 fat-jar：源码和 `lib/` 都保留，方便现场修改和重新编译。JUnit 等 test scope 依赖不会放入 `lib/`。

## 八、正式报告模式

推荐使用表清单文件：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <default-schema|->
--table-file <table-file> <output-root> [fetchSize]
```

假设把达梦 JDBC Driver 放入发行包 `lib/`，Windows / Git Bash 示例：

```bash
java -cp "quality-analysis.jar;lib/*" \
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli \
  dm.jdbc.driver.DmDriver \
  "jdbc:dm://127.0.0.1:5236/DAMENG" \
  USER PASSWORD DM_TEST TEST \
  --table-file "config/tables.txt" \
  "D:/quality-runs" \
  10000
```

如果表清单全部写完整 `SCHEMA.TABLE`：

```bash
java -cp "quality-analysis.jar;lib/*" \
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli \
  dm.jdbc.driver.DmDriver \
  "jdbc:dm://127.0.0.1:5236/DAMENG" \
  USER PASSWORD DM_TEST - \
  --table-file "config/tables.txt" \
  "D:/quality-runs" \
  10000
```

为了兼容之前的方式，仍支持同一 Schema 下的逗号列表：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <schema>
<table1,table2,...> <output-root> [fetchSize]
```

旧的单表控制台模式也仍然保留：

```text
<driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]
```

## 九、Mock 与本地验收

不需要数据库即可预览最终报告：

```bash
bash scripts/mock-report.sh
```

会生成：

```text
target/mock-profile-report/
├── manifest.json
├── tables/
├── quality-profile.xlsx
├── quality-profile.html
└── quality-profile-tables/
```

完整本地验收说明：

```text
docs/LOCAL_TEST.md
```

Mock 数据说明：

```text
docs/MOCK_PREVIEW.md
docs/MOCK_TESTSET.md
```

## 十、核心模型

```text
ColumnMetadata      字段的 JDBC / 数据库元数据
ColumnProfile       一个字段实际探查出来的画像
TableMetadata       一张表及其字段定义
TableProfile        一张表扫描结束后的内存结果
TableProfileRecord  稳定的持久化/报告 DTO
RunManifest         一次任务的进度和元数据
TableRef            明确指定的一张 Schema.Table
```

正式主线以 `main` 为准。
