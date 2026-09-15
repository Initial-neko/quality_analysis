# 本地运行与验收说明

本文用于验收 Profile-only V1。正式主线以 `main` 为准。

## 一、环境要求

- JDK 8+
- Maven 3.x
- Windows 下建议使用 Git Bash
- 达梦 JDBC Driver 仅在实库运行时需要

先确认：

```bash
java -version
javac -version
mvn -version
```

## 二、拉取代码

```bash
git checkout main
git pull origin main
```

## 三、不接数据库先看 Mock 报告

```bash
bash scripts/mock-report.sh
```

生成：

```text
target/mock-profile-report/
├── manifest.json
├── tables/
├── quality-profile.xlsx
└── quality-profile.html
```

直接打开 Excel 和 HTML 即可。

## 四、完整回归 + 一条命令打包

执行：

```bash
mvn clean package
```

该命令会：

1. 编译 Java 8 代码；
2. 执行全部单元/回归测试；
3. 生成主程序 JAR；
4. 收集全部 runtime 依赖到 `lib/`；
5. 带上源码、文档、脚本和配置示例；
6. 同时生成目录版和 ZIP 版发行包。

最终产物：

```text
target/quality-analysis-0.1.0-SNAPSHOT-distribution/
├── quality-analysis.jar
├── lib/
├── src/
├── config/
│   └── tables.txt.example
├── docs/
├── scripts/
├── pom.xml
└── README.md

target/quality-analysis-0.1.0-SNAPSHOT-distribution.zip
```

看到：

```text
BUILD SUCCESS
```

即可认为代码、测试和发行包构建都成功。

## 五、表清单文件

正式运行时不扫描全库，而是明确提供要扫描的表。

复制示例：

```bash
cp config/tables.txt.example config/tables.txt
```

如果默认 Schema 为 `TEST`：

```text
# config/tables.txt
CUSTOMER
ORDERS
PRODUCT
```

也可以跨 Schema：

```text
TEST.CUSTOMER
SALES.SALES_ORDER
MASTER.PRODUCT
```

规则：

- UTF-8；
- 每行一张表；
- 空行忽略；
- 以 `#` 开头的整行注释忽略；
- 重复表自动去重；
- `TABLE` 使用默认 Schema；
- `SCHEMA.TABLE` 使用显式 Schema；
- 默认 Schema 参数传 `-` 时，每行必须写 `SCHEMA.TABLE`；
- 程序不会查询全库表清单，也不会扫描文件之外的表。

详见：

```text
docs/TABLE_LIST.md
```

## 六、准备达梦 JDBC Driver

发行包不会携带达梦 JDBC Driver。

现场拿到驱动后，最简单的方式是直接复制到发行包：

```text
lib/DmJdbcDriver18.jar
```

这样运行时统一使用：

```text
quality-analysis.jar;lib/*
```

## 七、正式报告模式

推荐命令格式：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <default-schema|->
--table-file <table-file> <output-root> [fetchSize]
```

在发行包目录中运行，例如：

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

如果 `tables.txt` 全部使用完整的 `SCHEMA.TABLE`：

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

`database-label` 只用于报告展示，不参与 JDBC 连接。

## 八、运行结果检查

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

重点确认：

- `manifest.json` 中 planned/success/failed 表数正确；
- 一张成功表对应一个 JSON；
- 文件清单之外没有额外表结果；
- Excel/HTML 与 JSON 中的 rowCount、NULL、Distinct 等一致；
- HTML 可以直接双击打开；
- 没有 Rule/PASS/FAIL/质量分。

## 九、失败恢复验证

可以故意在表清单中加入不存在的表：

```text
CUSTOMER
NOT_EXISTS
ORDERS
```

期望：

- CUSTOMER 完成后已经落盘；
- NOT_EXISTS 被 manifest 记录失败；
- ORDERS 继续执行；
- 已完成表结果不丢失；
- 如果至少有成功表，仍能生成报告。

## 十、建议第一轮真实环境规模

建议逐步扩大：

```text
1 张小表
→ 2~3 张典型表
→ 含 VARCHAR / NUMBER / DATE / CLOB / BLOB 的表
→ 20 万行级表
→ 再逐步扩大
```

观察：

- 执行时间；
- Java 内存；
- 数据库负载；
- JSON 大小；
- Excel/HTML 打开速度。

## 十一、验收标准

```text
[ ] bash scripts/mock-report.sh 成功
[ ] mvn clean package 成功
[ ] distribution 目录存在 jar/lib/src/config/docs/scripts
[ ] ZIP 发行包生成成功
[ ] 表清单文件解析正确
[ ] 只扫描清单中指定的表
[ ] 每张完成表立即保存 JSON
[ ] manifest 成功/失败表数正确
[ ] Excel 正常
[ ] HTML 正常
[ ] CLOB/BLOB 没有明显内存异常
[ ] 不出现 Rule / PASS / FAIL / 质量分
```
