# Excel 字段明细列配置

## 一、为什么要改

之前 `字段质量明细` 的列定义分散在 `ExcelProfileReportWriter` 多个位置：

- `DETAIL_HEADERS` 决定表头；
- `writeFieldDetails()` 手工按位置写每一列；
- 单独的 `widths` 数组决定列宽；
- AutoFilter 又依赖表头数组长度。

因此删除一列时必须同时修改多处，而且很容易出现“表头删了但数据没有删”“后面列整体错位”“列宽和字段对不上”等问题。

## 二、当前结构

现在每一列都集中定义在：

```text
src/main/java/com/initialneko/qualityanalysis/report/ExcelDetailColumn.java
```

一列同时包含：

- 表头名称；
- 默认列宽；
- 该列如何从 `TableProfileRecord` / `ColumnRecord` 取值；
- 百分比、换行等显示方式。

真正决定 `字段质量明细` 默认输出哪些列、以及顺序的地方只有：

```text
ExcelProfileReportWriter.DETAIL_COLUMNS
```

例如当前类似：

```java
private static final ExcelDetailColumn[] DETAIL_COLUMNS = {
    ExcelDetailColumn.DATABASE,
    ExcelDetailColumn.SCHEMA,
    ExcelDetailColumn.TABLE,
    ExcelDetailColumn.COLUMN,
    ExcelDetailColumn.DB_TYPE,
    ...
    ExcelDetailColumn.INSIGHTS
};
```

如果后续不需要 `总行数` 和 `最小值`，只删除：

```java
ExcelDetailColumn.ROW_COUNT,
ExcelDetailColumn.MIN_VALUE,
```

即可。

表头、行数据、列宽和 AutoFilter 会一起调整，不需要再同步修改其他数组或列序号。

如果只是调整顺序，也只移动 `DETAIL_COLUMNS` 中对应项。

## 三、为什么暂时不做外部配置文件

V1 的目标仍然是轻量、可携带、最小配置。当前主要诉求是让开发人员能够快速裁剪交付列，而不是让最终用户在运行时自由组合 Excel 模板。

因此当前先采用“代码内单点配置”，不增加：

- 新的 properties/yaml；
- CLI 参数；
- 模板解析器；
- 动态表达式。

如果后续现场确实出现多套长期并存的 Excel 模板，再考虑把 `DETAIL_COLUMNS` 外置成配置。

## 四、DB 类型展示

JDBC 扫描阶段本来就已经保存：

```text
columnTypeName
precision
scale
jdbcType
```

报告层现在通过 `DatabaseTypeFormatter` 使用这些已有元数据，不增加数据库查询。

典型展示：

```text
VARCHAR + precision=100     -> VARCHAR(100)
CHAR + precision=10         -> CHAR(10)
DECIMAL + 18,2              -> DECIMAL(18,2)
NUMBER + 20,0               -> NUMBER(20,0)
INTEGER + precision=10      -> INTEGER
```

最后一条很重要：JDBC 对 INTEGER 也可能返回数值精度，但 `INTEGER(10)` 容易被误解成数据库 DDL 中声明了长度，因此当前只对字符/二进制定长类型显示长度，对 DECIMAL/NUMERIC 显示精度和小数位。

如果数据库驱动没有返回有效 precision，则保持原始类型名，例如：

```text
VARCHAR
```

而不会人为补一个长度。
