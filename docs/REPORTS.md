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

Java Serialization 不透明，并且和类版本绑定较紧。JSON 更容易人工查看、diff、排查问题，也方便其他语言继续消费。

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
├── quality-profile.html
└── quality-profile-tables/
    ├── TEST.CUSTOMER.html
    ├── TEST.ORDERS.html
    └── ...
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
- 预设正则校验结果 `presetValidation`（命中预设类型的列才有值）

正式 JSON 写入前先写 `*.tmp`，成功后再替换正式文件，避免中断产生半写文件。

### 缺失值进入 Profile 的边界

当前把以下三类值都视为“缺失类值”：

1. 数据库物理 `NULL`
2. trim 后为空的空串
3. 语义空值，默认 `NULL`、`N/A`、`NA`

它们仍分别保留 `nullCount`、`blankCount`、`semanticNullCount` 计数，但识别后不会继续进入：

- Distinct
- 唯一率分母
- 枚举 / TopN 频次
- Min / Max
- 字符串长度
- Pattern
- String Shape
- Case Variant
- Relationship Fingerprint

`nonNullCount` 仍保留 JDBC 层“物理非 NULL”的原始含义，因此空串和语义空值仍属于物理非 NULL，但不会污染后续画像指标。

## 六、Excel 报告

文件：

```text
quality-profile.xlsx
```

当前三个 Sheet：

### 1. 扫描概览

展示任务基本信息，以及计划/成功/失败表数、字段总数、累计扫描行数、潜在枚举、候选唯一键、常量字段、fetchSize 等。

### 2. 字段质量明细

明细列由 `DetailRow` 上的注解统一驱动（见 `docs/EXCEL_COLUMNS.md`），当前为 22 列：

```text
数据库
Schema
表名
字段名
DB类型
主键
总行数
NULL数
NULL率
空串/语义空
Distinct数
唯一率
最小值
最大值
长度(最小/最大/平均)
枚举/TopN
探查提示
预设类型
预设匹配率
预设匹配数
预设不匹配数
不匹配样本
```

内部 JDBC 类型号、ValueFamily、nullable、nonNullCount、LOB 内部标记等继续保存在 JSON，不在主交付 Sheet 中铺开。

### 3. 探查提示

只展示 Profile 自动发现的提示：

- 潜在枚举
- 候选唯一键
- 常量字段
- 准常量字段

这些都属于“发现”，不是质量校验失败。

## 七、HTML 报告

入口文件：

```text
quality-profile.html
```

入口页只展示：

- 整体扫描摘要卡片
- 任务元数据
- 可搜索表概览
- 每张表对应的详情链接

字段明细不再全部塞进入口页，而是拆成：

```text
quality-profile-tables/<schema>.<table>.html
```

每个表页面只渲染自己这一张表的字段 Profile，并提供返回总览链接。

这样表数量、字段数量较大时，浏览器不会一次创建所有字段 DOM，入口 HTML 文件也不会随着全部字段明细线性膨胀。

所有页面仍然是纯静态文件，不需要 Spring、Tomcat、Nginx、Node 或其他 Web Server。直接双击 `quality-profile.html` 即可使用，相对链接会打开对应表页面。

## 八、预设正则类型识别与校验

这是对 V1 "只探查不校验" 边界的**一个确认过的范围例外**（见 `docs/DESIGN.md` §十三）。

### 机制

1. **取样识别**：取每张表扫描的前 N 行（默认 10，`ProfileOptions.presetSampleSize`，通过单遍扫描内截取实现，不改扫描 SQL）。
2. **类型判定**：对每个字符串列，用内置预设正则（手机号/身份证/邮箱/日期时间/日期/URL/整数/小数/邮编，见 `regex/BuiltinPresetRules`）+ 可选自定义规则（JSON 配置，见 `config/preset-regex.json.example`）逐个匹配采样值。非空样本命中率 ≥ 阈值（默认 80%）且比率最高的规则获胜；并列取规则顺序首位。
3. **全列校验**：命中后，该列全部非空字符串值（NULL/空串/语义空不计入分母）继续参与正则校验，统计匹配数/不匹配数/匹配率，并保留最多 20 条不匹配样本。
4. **报告呈现**：字段质量明细（HTML + Excel）尾部追加 `预设类型 / 预设匹配率 / 预设匹配数 / 预设不匹配数 / 不匹配样本` 列。

### 语义边界

这仍然是**探查型标注**：`预设匹配率` 说明"该列长什么样"，不是 PASS/FAIL，不产生整改动作。自定义规则使用 JSON 文件 + `--preset-config <file>` 加载，非法正则会被跳过并告警；所有匹配均为全长锚定且限制输入长度（ReDoS 防护）。默认开启，可用 `--no-preset-validation` 关闭。

## 九、探查提示的语义边界

当前探查提示不是 Rule，也不是 PASS / FAIL。

### 潜在枚举

当前条件：

```text
LOB 正文未跳过
AND distinctCount > 0
AND distinctCount <= 20
```

它只表示值域比较小，适合人工进一步判断是不是代码/状态/类别字段。

### 候选唯一键

当前条件：

```text
有数据
AND 无物理 NULL
AND 无空串
AND 无语义空值
AND Distinct = 总行数
AND 非 LOB
```

这只是“数据表现得像唯一键”，不表示数据库已经声明 PK。

### 常量字段

有效值的 Distinct = 1。

例如所有有效记录都是 `CRM`，则会提示常量字段。

### 准常量字段

当前判断是最高频有效值占比 `>= 99%`。

它用于提示“几乎所有记录都一样”的字段。

### LOB 仅统计长度

HTML/Excel 字段明细还会显示 `LOB仅统计长度` / `LOB仅画像长度`，用于说明 CLOB/BLOB 正文没有参与 Distinct、TopN 等画像。这是处理策略提示，不属于四类正式探查发现。

## 十、最重要的业务边界

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

## 十一、单表重跑

单表重跑后，可以重新写同一个：

```text
<schema>.<table>.json
```

之后重新生成报告时会读取最新记录。HTML 重新生成时会先清理旧的表详情 HTML，避免留下已经不存在的旧页面。

当前 V1 尚未提供“选择失败表一键重跑”的 UI/命令，但存储结构已经支持未来增加这个能力。

## 十二、测试

`ProfilePersistenceAndReportTest` 验证：

1. 单表可以保存并重新读取；
2. manifest 进度独立保存；
3. 报告只从保存后的记录生成；
4. XLSX 包含正确 Sheet 和关键字段；
5. HTML 首页只保存表级概览；
6. 每张表生成独立详情 HTML，且相对链接可回到首页。

`MockProfileRegressionTest` 额外验证空串和语义空值不会再进入 Distinct、唯一率、长度和 TopN 等后续画像。
