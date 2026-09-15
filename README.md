# quality_analysis

一个轻量级的 Java 8 JDBC 数据探查工具。当前 V1 只做 **Profile（数据画像/探查）**：每张表只扫描一次，计算字段和表级画像；每完成一张表就立即持久化；最后从持久化结果生成 Excel 和 HTML 报告。

当前首先适配达梦（DM），核心扫描逻辑基于标准 JDBC，后续可以继续适配 Oracle、MySQL、PostgreSQL 等数据库。

## 一、V1 范围

V1 **不包含字段级 Rule 规则引擎**。Rule 需要根据具体字段配置业务规则，例如手机号正则、年龄范围、字典值等；当前第一版没有这个需求，因此只保留 Profile。

之前完整的 Profile + Rule 实现保留在：

```text
archive/profile-with-rules
```

除非产品范围重新明确需要规则，否则不要把 Rule 类重新混入 V1。

## 二、整体处理链路

```text
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
      `--> quality-profile.html
```

V1 不做应用层分页，不使用 OFFSET/LIMIT，也不要求按照主键排序。JDBC/达梦驱动负责结果集预取，程序使用 `ResultSet.next()` 顺序消费数据，不会把每个 fetch 批次先装进一个大 List。

主键信息通过 `DatabaseMetaData.getPrimaryKeys()` 尽力读取。主键只作为字段元数据标签，不是扫描前提。

## 三、任务目录与持久化

每一次扫描任务都是一个独立目录：

```text
runs/20260915_001/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   ├── TEST.ORDERS.json
│   └── ...
├── quality-profile.xlsx
└── quality-profile.html
```

- `manifest.json`：保存任务编号、数据库标签、开始/结束时间、成功/失败表数、ProfileOptions 快照等；**不会保存数据库密码**。
- `tables/*.json`：一张完成的表对应一个 JSON 文件。
- JSON 先写入 `*.tmp`，再尽量使用原子替换方式写正式文件，避免中断留下半个结果文件。
- 第 N 张表失败，不会影响前 N-1 张已经保存的结果。
- 单表重跑时可以直接覆盖对应 JSON，不需要重写整个任务结果。
- Excel/HTML 只读取这些持久化记录，不会重新扫描数据库，也不会重新计算字段指标。

V1 暂时不使用 SQLite。当前需求是本地、可携带、按表落盘的结果，使用数据库反而会增加部署和 schema 维护成本。未来如果出现多用户、历史趋势、跨任务查询等需求，再考虑数据库化。

## 四、默认 Profile 指标

默认只打开高价值、必要的画像能力：

- 总行数
- 物理 NULL 数和 NULL 率
- 空字符串数量
- 语义空值数量（默认识别 `NULL`、`N/A`、`NA`）
- 精确 Distinct 数
- 唯一率
- 低基数值及频次
- `distinct <= 20` 时输出全部枚举值、数量和占比
- 数值/日期 Min、Max
- 字符串/LOB 长度统计
- 数据库声明主键标签
- 候选唯一键标记
- 常量字段、准常量字段、潜在枚举字段

以下属于可选探查能力，默认关闭：

- Pattern 格式画像，例如 `D11`、`L2D9`、`D4-D2-D2`
- 字符组成统计
- 大小写变体分析
- Set Fingerprint / MinHash 关系发现
- CLOB 文本预览

只在确实需要时通过 `ProfileOptions` 显式开启。

## 五、CLOB / BLOB

LOB 单独处理，避免全表扫描时误把大字段内容全部加载到内存。

| 类型 | V1 行为 |
|---|---|
| CLOB / NCLOB | 默认只读取 `Clob.length()`；正文不读取；可选有限前缀预览 |
| BLOB | 只读取 `Blob.length()`；不加载字节内容 |
| VARCHAR / CHAR | 正常 Profile |

LOB 内容不参与 Distinct / 唯一率计算。

对于达梦，建议保持 `clobAsString=false`，让 CLOB 仍然能够被 JDBC 正确识别为 CLOB。达梦 JDBC 驱动由使用者自行提供，不提交到仓库。

## 六、报告

### Excel

生成文件：

```text
quality-profile.xlsx
```

目前包含三个 Sheet：

1. `扫描概览`：任务信息以及总体统计。
2. `字段质量明细`：核心交付 Sheet，一行一个字段。
3. `探查提示`：潜在枚举、候选唯一键、常量/准常量等 Profile Insight。

字段明细包括：数据库、Schema、表、字段、DB/JDBC 类型、主键标记、总行数、NULL/空串/语义空值、Distinct、唯一率、Min/Max、长度、枚举/TopN、候选唯一键、常量等。

### HTML

生成文件：

```text
quality-profile.html
```

这是一个可以直接双击打开的静态 HTML，包含：

- 总体扫描概览
- 可搜索的表目录
- 表级摘要
- 单表字段详情
- 枚举 / TopN / Profile Insight

不需要 Spring、Tomcat、Nginx、Node.js 或浏览器插件。

**V1 报告不出现 Rule PASS/FAIL，也不计算质量分。Profile 只描述“数据实际是什么样”，不擅自判断业务正确性。**

## 七、核心模型

```text
ColumnMetadata      字段的 JDBC / 数据库元数据
ColumnProfile       一个字段实际探查出来的画像
TableMetadata       一张表及其字段定义
TableProfile        一张表扫描结束后的内存结果
TableProfileRecord  稳定的持久化/报告 DTO
RunManifest         一次任务的进度和元数据
```

## 八、构建与测试

要求：JDK 8+、Maven 3.x。

```bash
mvn clean test
mvn clean package
```

当前运行依赖控制在：

- Gson 2.10.1：JSON 持久化
- Apache POI 5.2.2：生成 `.xlsx`

Profile 核心仍然是标准 Java/JDBC。没有 Maven 时，可以只验证无第三方依赖的核心 Profile：

```bash
bash scripts/manual-regression.sh
```

更完整的本地验收步骤见：

```text
docs/LOCAL_TEST.md
```

## 九、达梦实库运行

先编译并复制运行依赖：

```bash
mvn clean package
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
```

达梦 JDBC jar 不放进仓库，假设本地路径为：

```text
D:\dm\DmJdbcDriver18.jar
```

Windows / Git Bash 下报告模式示例：

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

报告模式参数：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <schema>
<table1,table2,...> <output-root> [fetchSize]
```

仍然保留旧的单表控制台模式：

```text
<driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]
```

## 十、Mock 回归基线

`MockDatasets` 是长期回归数据集。新增 Profile/报告能力时，优先增加确定性的 Mock 数据和断言，再接真实达梦。

当前测试已经覆盖：

- Profile 核心指标
- 每表 JSON 落盘和重新读取
- manifest 持久化
- Excel 三个 Sheet 及关键字段
- HTML 关键内容

详见：

```text
docs/MOCK_TESTSET.md
docs/REPORTS.md
```

## 十一、分支说明

```text
main                         V1 正式主线（Profile Only）
archive/profile-with-rules   保留的完整 Profile + Rule 历史实现
feature/profile-only-v1      本轮 V1 开发分支，合并后可作为开发记录保留
```
