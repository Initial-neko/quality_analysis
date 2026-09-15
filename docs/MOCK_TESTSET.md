# Mock 回归数据集

Mock 数据集是 **Profile-only V1** 的长期回归基线。任何新的 Profile 能力，在接真实达梦之前，都应该先增加确定性的 Mock 场景和断言。

字段级 Rule 不属于 V1。Rule 相关回归只保留在：

```text
archive/profile-with-rules
```

## 一、CUSTOMER（200 行）

| 字段 | 场景 / 期望 Profile |
|---|---|
| CUSTOMER_ID | 1..200；完全唯一；候选唯一键；同时带数据库声明主键标签 |
| STATUS | ACTIVE / INACTIVE / PENDING，另外包含 ` active ` 和 `Active`；用于低基数、trim、大小写污染场景 |
| GENDER | M / F / U；与 ORDERS 共享，可用于可选关系发现 |
| AGE | 正常年龄外加入 -1 和 150；Profile 只暴露 Min/Max，不判断异常 |
| PHONE | 大部分是 11 位数字，包含一个 `138-1234-5678`；可用于可选 Pattern 探查 |
| CREATE_DATE | 大部分为 2026-09-15，加入一个 2099-12-31；只验证范围 |
| SOURCE_SYSTEM | 全部为 CRM，用于常量字段 |
| NOTE | Mock CLOB，长度 101..300 |
| PAYLOAD | Mock BLOB，长度 2049..2248 |
| OPTIONAL_CODE | 同时包含物理 NULL、空串、语义 `NULL`，以及少量编码值 |

## 二、ORDERS（120 行）

| 字段 | 场景 |
|---|---|
| ORDER_ID | 唯一候选键 |
| CUSTOMER_ID | 取 CUSTOMER 1..120 的子集，可作为未来关系发现输入 |
| GENDER | M / F / U，与 CUSTOMER 共享 |
| ORDER_STATUS | 低基数枚举型数据 |

## 三、V1 核心测试必须验证什么

- 行数稳定
- 物理 NULL / 空串 / 语义空值数量
- 精确 Distinct
- 唯一率 / 候选唯一键
- 低基数值输出
- 数值 / 日期 Min、Max
- 长度统计
- LOB 正文不被物化
- Minimal Profile 的非必要开关默认关闭
- Pattern / 关系发现只在显式开启时工作

V1 **不能断言 PASS/FAIL**。

例如 AGE 中故意放入 -1 和 150，只是为了验证 Min/Max 能正确发现真实范围；它们是否非法，需要未来具体字段 Rule 才能决定。

## 四、持久化与报告回归

`ProfilePersistenceAndReportTest` 把持久化格式视为 V1 的正式契约之一：

1. CUSTOMER 转成 `TableProfileRecord`，保存为一个表级 JSON；
2. 重新读取 JSON，验证 rowCount、STATUS distinct、候选唯一键等关键指标；
3. manifest 独立保存和读取；
4. CUSTOMER + ORDERS 的持久化记录用于生成 `quality-profile.xlsx` 和 `quality-profile.html`；
5. Excel 必须包含 `扫描概览`、`字段质量明细`、`探查提示`，并包含关键表/字段；
6. HTML 必须包含总体报告、CUSTOMER、PHONE、潜在枚举等关键内容。

报告测试一定是“**先保存，再重新读取，然后生成报告**”，以保证报告 Writer 不访问源数据库，也不从原始行重新计算指标。

## 五、回归原则

- 只使用确定性数据，不用随机数据替代关键场景
- 每个 bug 修复尽量增加对应的确定性回归 Case
- 关键 Profile 指标必须有明确断言
- 真实达梦集成测试属于另一层，不能替代 Mock 回归
- 报告 Writer 消费持久化后的 `TableProfileRecord`，不消费 JDBC 原始行
