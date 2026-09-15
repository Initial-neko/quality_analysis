# 表清单文件说明

第一版正式运行时，程序**不会自动发现、枚举或扫描数据库中的全部表**。需要扫描哪些表，由使用者通过表清单文件明确提供。

## 一、推荐格式

文件使用 UTF-8 编码，每行一张表：

```text
# 默认 Schema 为 TEST
CUSTOMER
ORDERS
PRODUCT
```

空行会被忽略，以 `#` 开头的整行注释会被忽略。

如果同一个文件需要跨多个 Schema，可以写完整名称：

```text
TEST.CUSTOMER
TEST.ORDERS
SALES.SALES_ORDER
MASTER.PRODUCT
```

文件中重复出现同一张表时会自动去重，并保持第一次出现的顺序。

## 二、默认 Schema

命令行仍然保留一个 `default-schema` 参数。

例如：

```text
default-schema = TEST
```

则文件中的：

```text
CUSTOMER
```

会解释为：

```text
TEST.CUSTOMER
```

如果某一行已经写成：

```text
SALES.ORDERS
```

则使用该行显式指定的 `SALES`，不会使用默认 `TEST`。

如果不希望设置默认 Schema，把命令行该参数写成：

```text
-
```

此时文件中的每一张表都必须写成 `SCHEMA.TABLE`。

## 三、运行方式

正式报告模式：

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <default-schema|->
--table-file <table-file> <output-root> [fetchSize]
```

Windows / Git Bash 示例：

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

如果 `tables.txt` 全部使用完整 `SCHEMA.TABLE`：

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

## 四、重要边界

表清单只是输入选择器：

```text
表清单文件
   ↓
TableRef 列表
   ↓
逐表 Profile
```

程序不会执行“查询数据库所有表 -> 再进行过滤”的动作，因此不会意外扫描清单之外的表。

如果某张表不存在或无 SELECT 权限，该表会记录失败；前面已经完成的表结果不会丢失，程序仍会继续尝试后续表。

发行包中自带示例：

```text
config/tables.txt.example
```

复制成自己的 `tables.txt` 后修改即可。
