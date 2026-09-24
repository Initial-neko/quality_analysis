# V1 设计说明 —— Profile Only

## 一、范围

V1 明确只做 Profile（数据画像/探查），不包含字段级 Rule、RuleBinding、校验状态或 RuleResult。

完整的 Profile + Rule 版本保留在：

```text
archive/profile-with-rules
```

当前 V1 的开发实现来自：

```text
feature/profile-only-v1
```

合并后 `main` 作为正式 V1 主线。

## 二、设计约束

- Java 8
- 尽量少的依赖
- 第一适配数据库为达梦 DM
- 常见表约 20 万行，当前按约 100 万行以内设计
- 一张表只扫描一次，完成所有已开启的 Profile 指标
- 不做应用层分页
- 不要求主键排序
- 默认不物化 LOB 正文
- 非必要探查能力必须可以关闭
- 一张表完成后必须立即落盘
- 报告必须读取持久化记录，不能重新扫描源库

## 三、端到端链路

`JdbcTableProfiler` 对每张表执行一次 forward-only 扫描。`ProfileRunService` 按顺序处理多张表，并在进入下一张表之前先保存当前表结果。

```text
Database
   |
ResultSet（forward only + fetchSize）
   |
ProfileEngine
   |
TableProfile
   |
TableProfileRecord
   |
每表 JSON + RunManifest
   |
   +-- ExcelProfileReportWriter
   `-- HtmlProfileReportWriter
```

JDBC fetch 边界只是驱动层的数据传输细节，不是 Profile 的业务边界。

## 四、JDBC 扫描路径

一张表的执行过程：

1. 尽力通过 `DatabaseMetaData.getPrimaryKeys()` 获取主键信息；
2. 执行 `SELECT * FROM schema.table`；
3. 使用 `TYPE_FORWARD_ONLY` + `CONCUR_READ_ONLY`；
4. 使用可配置的 `Statement.setFetchSize(...)`；
5. `ResultSetMetaData` 转为 `TableMetadata / ColumnMetadata`；
6. 每个 cell 只被 `ProfileEngine` 消费一次；
7. 扫描完成得到 `TableProfile`；
8. 转成 `TableProfileRecord` 并立即保存。

V1 不使用 OFFSET/LIMIT，也不需要 `ORDER BY` 主键。

## 五、核心模型

### ColumnMetadata

表示字段物理元数据：

- 字段名 / label
- JDBC 类型
- 数据库原生类型名
- precision / scale
- 是否允许 NULL
- 数据库声明的主键标记
- 归一化后的 `ValueFamily`

### ColumnProfile

表示“字段实际数据长什么样”，而不是“字段是否符合业务规则”。

核心指标包括：

- row / non-null / null 数量
- 空字符串、语义空值数量
- 精确 Distinct
- 唯一率
- 低基数值及频次
- Min / Max
- 最小/最大/平均长度
- 候选唯一键
- 常量 / 准常量
- 潜在枚举

可选指标包括：

- Pattern 指纹
- 字符组成统计
- 大小写变体
- Set Fingerprint / MinHash

### TableProfile

Profile 引擎内部的一张表扫描结果：

```text
TableMetadata
rowCount
List<ColumnProfile>
```

### TableProfileRecord

稳定的持久化/报告 DTO。

它只保存报告和后续处理需要的字段元数据与 Profile 指标，避免把文件格式、Excel/HTML 等关注点侵入 Profile 核心。

### RunManifest

一次扫描任务的持久化元数据：

- runId
- 非敏感数据库标签
- 开始/结束时间
- 任务状态
- 计划表数 / 成功表数 / 失败表数
- ProfileOptions 快照
- 每张尝试扫描表的执行记录

**不会保存数据库用户名密码之外的敏感连接凭据，尤其不会保存密码。**

## 六、持久化边界

V1 一张完成的表保存一个 JSON：

```text
<run>/tables/<schema>.<table>.json
```

一次任务保存一个：

```text
<run>/manifest.json
```

文件先写 `*.tmp`，在文件系统支持时再使用原子替换写正式文件。这样即使后面的表失败或进程中断，已经完成的表结果仍然安全存在。

### 为什么不用 JSONL

JSONL 很适合追加事件，但我们的执行和重跑单位是“表”。如果一张表重跑，JSONL 需要追加新版本后再去重，或者重写整个文件；一表一 JSON 更直接。

### 为什么暂时不用 SQLite

SQLite 在以下场景会有价值：

- 多次历史任务查询
- 多用户共享
- 跨任务趋势分析
- 大量条件查询
- 并发写入

当前 V1 只需要本地可靠落盘和可携带交付，引入 SQLite 会增加部署、schema、迁移和兼容性成本，因此暂时不用。

## 七、精确 Distinct

当前 20 万～100 万行的目标规模下，V1 对开启 Distinct 的普通字段使用精确 `HashSet`。

优点：

- 结果确定
- 唯一率容易计算
- 潜在枚举容易判断
- 不引入 HLL 等额外复杂度

内存控制原则：

- 默认按表串行处理
- 表完成后先保存，再进入下一张表
- 表结果保存后释放本表 accumulator
- LOB 正文不做 Distinct
- 低基数频次 Map 到达上限后不再无限增长
- 关系指纹等可选能力默认关闭

没有实际内存数据证明有必要前，不引入 HLL。

## 八、低基数处理

默认逻辑：

```text
distinct <= 20      输出全部值 + 数量 + 占比
20 < distinct <= N  保留有限低基数频次 / Top N
高基数字段           不长期保存全部值频次
```

低基数只是探查结果，例如“潜在枚举”，不是质量异常。

## 九、LOB 处理

### CLOB / NCLOB

- 调用 `Clob.length()`
- 默认不读取正文
- 只有显式开启时才读取有限前缀预览

### BLOB

- 调用 `Blob.length()`
- 不把字节内容物化到内存

LOB 正文不参与 Distinct / 唯一率。

## 十、可选探查能力

以下不是 Minimal Profile 的必要结果，默认关闭：

- Pattern 指纹
- 字符组成统计
- 大小写变体
- 关系 Fingerprint / MinHash
- CLOB Preview

它们可以用于专项探查，但 V1 Excel/HTML 报告不能依赖这些能力才能正常工作。

## 十一、报告边界

报告位于持久化之后：

```text
manifest.json + tables/*.json
        |
        +-- quality-profile.xlsx
        `-- quality-profile.html
```

报告 Writer 只能格式化和汇总已经保存的 Profile 数据：

- 不重新查询数据库
- 不重新扫描原始数据
- 不再计算第二套字段指标
- 不生成不存在的业务校验状态

### Excel V1

- 扫描概览
- 字段质量明细：一行一个字段
- 探查提示 / 枚举信息

### HTML V1

- 整体任务概览
- 可搜索表目录
- 表级概览
- 单表字段详情

Apache POI 5.2.2 只用于 XLSX 渲染；Gson 只用于 JSON 持久化。Profile 核心仍然是 Java/JDBC。

## 十二、执行服务

`ProfileRunService` 负责：

1. 创建 run 目录和 RUNNING 状态 manifest；
2. 扫描一张表；
3. 保存该表 `TableProfileRecord`；
4. 立即更新 manifest；
5. 继续下一张表；
6. 所有表结束后更新最终 manifest；
7. 从持久化记录生成 Excel 和 HTML。

当前实现遇到单表 `SQLException` 后会继续下一张表；如果所有表都失败，会在持久化任务信息后重新抛出第一条 SQL 异常。

## 十三、明确延期

除非范围变化，V1 不加入：

- Rule 规则引擎
- 字典 / 范围 / 正则校验（部分例外，见下）
- PASS / WARN / FAIL
- 质量评分
- HLL / 近似 Distinct
- Sampling（部分例外，见下）
- 大规模并行扫描
- DB 侧规则下推
- 历史趋势数据库
- 整改工作流

> **经确认的范围例外（2026-09）**：应需求方要求，V1 引入了「预设正则类型识别 + 校验」——
> 取数据集前 N 行（默认 10，`presetSampleSize`）判断各字符串列是否符合内置/自定义正则预设
> （手机号、身份证、邮箱等），命中后对全列做校验并输出匹配率。这与完整的 Rule 引擎 / PASS-FAIL
> 体系仍然不同：它是"探查型"标注而非规则校验框架。可通过 `--no-preset-validation` 关闭。
> 取样通过单遍扫描内截取前 N 行实现，不修改 `SELECT *` 扫描 SQL。

Rule 相关实现继续保留在：

```text
archive/profile-with-rules
```
