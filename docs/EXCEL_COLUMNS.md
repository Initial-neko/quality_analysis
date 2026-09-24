# 字段质量明细列配置（注解驱动）

## 一、演进过程

- 最早：列定义分散在 `ExcelProfileReportWriter` 多个位置（表头数组、手工按位写值、列宽数组、AutoFilter），删列要同步改多处。
- 第一次重构：集中到 `ExcelDetailColumn` 枚举（已在本次改造中移除）。
- **当前：注解驱动的通用渲染框架**。列定义在实体类的注解里，HTML 与 Excel 由同一份列描述符渲染，从架构上保证两个出口不会漂移。

## 二、当前结构

明细表的"实体类"是：

```text
src/main/java/com/initialneko/qualityanalysis/report/DetailRow.java
```

每一列是一个 `@DerivedColumn` 方法，同时声明：

- `order`：列顺序（升序渲染）；
- `header`：表头名称；
- `width`：Excel 列宽（字符数）；
- `type`：单元格语义（`TEXT / NUMBER / PERCENT / WRAP / DATE`），同时驱动 HTML 格式化与 Excel 样式。

例如：

```java
@DerivedColumn(order = 25, header = "NULL率", width = 12, type = CellType.PERCENT)
public Double nullRate() { return column.nullRate; }
```

增删/调序/改名/改宽度都只改这一个方法；HTML 明细表和 Excel 明细 Sheet 会同步变化。

## 三、渲染链路

```text
DetailRow（注解）
   |
ReportDescriptorBuilder.scan(DetailRow.class)
   |
ReportDescriptor（列描述符：表头/顺序/宽度/类型/取值器）
   |
   +-- HtmlRenderer.renderTable(rows, descriptor)   -> HTML 明细 <table>
   `-- ExcelRenderer.renderSheet(...)               -> Excel 明细 Sheet
```

框架本身是通用的：任何实体类只要加上 `@ReportTable / @ReportColumn / @DerivedColumn / @Flatten` 注解，交给 `ReportEngine.descriptor(Class)` 即可渲染出 HTML 表格与 Excel Sheet，不限于 `DetailRow`。

## 四、预设正则校验列

明细表尾部有 5 个预设校验列（`预设类型 / 预设匹配率 / 预设匹配数 / 预设不匹配数 / 不匹配样本`），由 `ColumnRecord.presetValidation` 派生，同样只是 `DetailRow` 上的注解方法——这正是"加列只加注解"的落地验证。详见 `docs/REPORTS.md`。

## 五、为什么仍然不做外部模板配置

结论与之前一致：保持轻量、零新依赖。注解已经是"代码内单点配置"，若现场出现多套长期并存的 Excel 模板需求，再考虑外置。
