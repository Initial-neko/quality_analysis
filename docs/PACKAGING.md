# 发布包打包说明

当前 V1 不使用 fat-jar。原因是现场可能需要查看和修改源码，因此发布包同时保留：

- 主程序 JAR
- `lib/` 运行依赖
- `src/main` 和 `src/test` 源码
- Maven `pom.xml`
- 中文文档
- 辅助脚本

## 一、一个命令完成打包

在项目根目录执行：

```bash
mvn clean package
```

这个命令会同时执行单元测试、编译、生成主 JAR，并生成可携带的 distribution 发布包。

## 二、发布包结构

发布内容结构如下：

```text
quality-analysis-0.1.0-SNAPSHOT-distribution/
├── quality-analysis.jar
├── lib/
│   ├── gson-2.10.1.jar
│   ├── poi-5.2.2.jar
│   ├── poi-ooxml-5.2.2.jar
│   └── Apache POI 的其他运行依赖...
├── src/
│   ├── main/
│   ├── test/
│   └── assembly/
├── docs/
├── scripts/
├── pom.xml
└── README.md
```

同时会生成同内容的 ZIP，便于整体拷贝到其他机器。

`lib/` 只包含运行时依赖，不包含 JUnit 等仅测试阶段使用的依赖。

## 三、为什么不用 fat-jar

当前使用场景不仅是“拿到一个包直接运行”，还包括现场修改代码。

如果做成 fat-jar：

- 第三方依赖全部塞进一个大 JAR，不方便确认和替换；
- 修改源码仍然必须另外准备源码工程；
- 现场排查依赖冲突不够直观。

现在的发布包中源码、依赖和主程序彼此分离，更适合内网和现场调试。

## 四、修改源码以后重新构建

进入解压后的发布目录，直接执行：

```bash
mvn clean package
```

即可重新编译并重新生成完整发布包。

因此发布 ZIP 本身就是一个可以继续开发的 Maven 工程。

## 五、达梦 JDBC 驱动

达梦 JDBC Driver 不提交仓库，也不会由 Maven 自动下载。

现场拿到达梦 JDBC jar 后，可以直接放入：

```text
lib/
```

例如：

```text
lib/DmJdbcDriver18.jar
```

也可以保持在其他目录，在启动时额外加入 classpath。

## 六、Windows / Git Bash 运行示例

如果达梦驱动已经放进 `lib/`，报告模式可以这样运行：

```bash
java -cp "quality-analysis.jar;lib/*" \
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli \
  dm.jdbc.driver.DmDriver \
  "jdbc:dm://127.0.0.1:5236/DAMENG" \
  USER PASSWORD DM_TEST TEST \
  "CUSTOMER,ORDERS" \
  "D:/quality-runs" \
  10000
```

Linux/macOS classpath 分隔符使用 `:`：

```bash
java -cp "quality-analysis.jar:lib/*" ...
```

## 七、Mock 报告仍然可用

没有数据库时，仍然可以在源码目录执行：

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

## 八、正式交付建议

现场建议拷贝整个 distribution ZIP，而不是只拷贝 `quality-analysis.jar`。

这样可以同时保留：

1. 可运行程序；
2. 完整依赖；
3. 源码；
4. Maven 构建文件；
5. 运行和验收文档。

后续现场需要修改时，不需要重新寻找对应版本源码和依赖。
