# Profile 持久化与报告说明

## 一、目标

V1 按表顺序扫描。一张表完成后必须立即变成可恢复的持久化结果，后面的表失败不能破坏前面已经完成的结果。

整体职责分层：

```text
JDBC 扫描 -> Profile -> 持久化 -> 报告渲染
```

报告渲染阶段不会重新访问源数据库。

## 二、为什么一张表一个 JSON

V1 每张表保存一个 `TableProfileRecord`，而不是使用 JSONL、Java Serialization 或 SQLite。

### 相比 JSONL

JSONL 适合追加式事件日志，但我们的重跑单位是“表”。如果一张表重新扫描，JSONL 需要追加新版本再做去重，或者重写整个文件。

一表一 JSON 和实际执行单元完全一致，更适合：

- 单表覆盖
- 单表重跑
- 断点恢复
- 独立检查
- 文件级归档

### 相比 Java Serialization

Java Serialization 不透明，并且和类版本绑定较紧。JSON 更容易：

- 人工查看
- diff
- 排查问题
- 被其他语言/工具继续消费

### 相比 SQLite

当系统未来需要历史查询、多用户、跨任务对比、并发写入时，SQLite 或服务数据库会更合适。

当前 V1 只需要本地可靠保存和交付文件，因此数据库会额外引入部署、schema 和迁移成本，没有必要。

## 三、任务目录

```text
<output-root>/<runId>/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   ├── TEST.ORDERS.json
│   └── ...
├── quality-profile.xlsx
└── quality-profile.html
```

`runId` 使用时间戳生成。数据库密码等敏感凭据不会写入任务目录。

## 四、manifest.json

`RunManifest` 保存：

- runId
- 非敏感 databaseLabel
- 开始时间 / 结束时间
- `RUNNING / COMPLETED / COMPLETED_WITH_ERRORS`
- 计划表数
- 成功表数
- 失败表数
- 关键 `ProfileOptions`
- 每张尝试扫描表的执行记录

每完成一张表都会重新写一次 manifest，采用临时文件 + 尽可能原子替换的方式。

因此如果程序在第 18 张表中断，前 17 张表的 JSON 和 manifest 进度仍然可用。

## 五、TableProfileRecord

持久化记录故意和 `TableProfile` 分开。

- `TableProfile`：Profile 引擎内部模型
- `TableProfileRecord`：稳定的文件/报告 DTO

这样 Excel/HTML、JSON 格式等关注点不会污染核心 Profile 引擎。

一张表的记录包括：

- database / schema / table
- 扫描时间 / 耗时 / 行数
- 字段 JDBC / 数据库元数据
- NULL / 空串 / 语义空值
- Distinct / 唯一率
- Min / Max
- 字符串 / LOB 长度
- Profile 已保留的低基数值和精确频次
- 潜在枚举
- 候选唯一键
- 常量 / 准常量
- 可选 Pattern / 字符组成结果（仅在相关开关开启时）

正式 JSON 写入前先写 `*.tmp`，成功后再替换正式文件，避免中断产生半写文件。

## 六、Excel 报告

文件：

```text
quality-profile.xlsx
```

当前三个 Sheet：

### 1. 扫描概览

展示：

- 计划 / 成功 / 失败表数
- 已保存表记录数
- 字段总数
- 累计扫描行数
- 潜在枚举字段数
- 候选唯一键数
- 常量字段数
- fetchSize
- 任务基本信息

### 2. 字段质量明细

这是主要交付 Sheet，一行一个字段。

字段包括：

- database / schema / table / field / label
- 数据库类型 / JDBC 类型 / ValueFamily
- 是否 nullable
- 是否数据库声明主键
- rowCount
- NULL / NULL率 / 空串 / 语义空值 / non-null
- Distinct / 唯一率
- Min / Max
- 最小 / 最大 / 平均长度
- 潜在枚举
- 枚举 / TopN 值
- 候选唯一键
- 常量 / 准常量
- 是否跳过 LOB 正文

该 Sheet 主要用于治理人员筛选、排序和后续分析。

### 3. 探查提示

只展示 Profile 自动发现的提示：

- 潜在枚举
- 候选唯一键
- 常量字段
- 准常量字段

这些都属于“发现”，不是质量校验失败。

## 七、HTML 报告

文件：

```text
quality-profile.html
```

HTML 是一个自包含的静态文件，CSS/JavaScript 都内嵌，可以直接双击打开。

目前提供：

- 整体扫描摘要卡片
- 任务元数据
- 可搜索表目录
- 表级概览
- 每张表的字段 Profile 明细
- 枚举 / TopN
- Profile Insight 标签

不需要任何服务端运行环境。

## 八、最重要的语义边界

V1 没有字段级 Rule。

例如：

```text
AGE min=-1 max=150
```

V1 只报告观察到的范围 `-1 ~ 150`，不会说 `-1` 是异常，因为没有业务规则说明 AGE 必须大于等于 0。

同样：

```text
distinct=5
```

可能产生“潜在枚举”提示，但这不是 PASS/FAIL。

未来如果重新启用规则能力，可参考：

```text
archive/profile-with-rules
```

## 九、单表重跑

单表重跑后，可以重新写同一个：

```text
<schema>.<table>.json
```

之后重新生成报告时会读取最新记录。

当前 V1 尚未提供“选择失败表一键重跑”的 UI/命令，但存储结构已经支持未来增加这个能力。

## 十、测试

`ProfilePersistenceAndReportTest` 验证：

1. 单表可以保存并重新读取；
2. manifest 进度独立保存；
3. 报告只从保存后的记录生成；
4. XLSX 包含正确 Sheet 和关键字段；
5. HTML 包含关键表、字段和 Profile 信息。

测试特意先把结果落盘，再重新读取 JSON 后生成报告，从架构上保证报告层不会偷偷重新扫描数据库。
