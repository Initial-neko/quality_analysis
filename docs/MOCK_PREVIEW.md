# Mock 报告预览说明

如果当前没有达梦数据库，可以直接使用仓库内置的确定性 Mock 数据生成完整 Profile 报告，不需要任何 JDBC Driver，也不需要连接数据库。

## 一、Mock 数据包含什么

当前有两张测试表。

### CUSTOMER：200 行，10 个字段

| 字段 | 测试场景 |
|---|---|
| CUSTOMER_ID | 1～200，唯一，声明为主键，可识别候选唯一键 |
| STATUS | ACTIVE / INACTIVE / PENDING，并故意加入 ` active `、`Active` |
| GENDER | M / F / U，小枚举 |
| AGE | 普通年龄，同时故意加入 -1 和 150；V1 只展示 Min/Max，不判异常 |
| PHONE | 大部分为 11 位数字，故意加入一条 `138-1234-5678` |
| CREATE_DATE | 大部分为 2026-09-15，故意加入一条 2099-12-31 |
| SOURCE_SYSTEM | 全部为 CRM，用来验证常量字段 |
| NOTE | Mock CLOB，只统计长度 101～300 |
| PAYLOAD | Mock BLOB，只统计长度 2049～2248 |
| OPTIONAL_CODE | 同时包含数据库 NULL、空串、语义空值 `NULL`、C0～C3 |

### ORDERS：120 行，4 个字段

| 字段 | 测试场景 |
|---|---|
| ORDER_ID | 唯一订单号 |
| CUSTOMER_ID | 1～120，模拟 CUSTOMER_ID 的子集 |
| GENDER | M / F / U |
| ORDER_STATUS | WAITING / DONE / CANCELLED，小枚举 |

Mock 数据是固定生成的，不使用随机数，因此每次运行结果应保持一致。

## 二、一条命令生成完整报告

在项目根目录执行：

```bash
bash scripts/mock-report.sh
```

脚本会自动执行 Maven test-compile、构造 Mock Profile、持久化 JSON，并生成 Excel / HTML。

默认输出目录：

```text
target/mock-profile-report/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   └── TEST.ORDERS.json
├── quality-profile.xlsx
└── quality-profile.html
```

也可以自己指定目录：

```bash
bash scripts/mock-report.sh D:/quality-mock
```

## 三、建议先看什么

### 1. HTML

直接双击：

```text
quality-profile.html
```

重点看：

- 总体扫描概览
- CUSTOMER / ORDERS 表列表
- CUSTOMER 的 10 个字段
- STATUS / GENDER / ORDER_STATUS 的潜在枚举信息
- CUSTOMER_ID / ORDER_ID 的候选唯一键信息
- SOURCE_SYSTEM 的常量字段提示
- AGE 的 Min=-1、Max=150
- CREATE_DATE 的最大值 2099-12-31
- NOTE / PAYLOAD 的长度信息

### 2. Excel

打开：

```text
quality-profile.xlsx
```

重点看三个 Sheet：

```text
扫描概览
字段质量明细
探查提示
```

`字段质量明细` 是当前最重要的交付格式，一行一个字段。

### 3. JSON

如果想确认报告不是重新计算得到的，可以打开：

```text
tables/TEST.CUSTOMER.json
```

Excel 和 HTML 都直接读取这些已经持久化的字段画像。

## 四、需要注意的语义

当前 V1 是 Profile-only。

例如 Mock 中：

```text
AGE min=-1 max=150
```

这里只说明真实观察到的范围是 -1～150，**不会自动判定 -1 或 150 为异常**。

同样，PHONE 中存在：

```text
138-1234-5678
```

默认 V1 只做基础 Profile；手机号 Regex Rule 属于以后字段规则能力，不属于当前版本。

## 五、Mock 报告使用的是当前默认 Profile 配置

`scripts/mock-report.sh` 使用：

```java
ProfileOptions.defaults()
```

因此生成的是当前正式 V1 默认效果，而不是为了单元测试额外开启 Pattern / MinHash 等高级探查能力。

这份 Mock 报告就是在没有真实数据库时验收 Excel、HTML、JSON 持久化和 Profile 指标展示的标准方式。
