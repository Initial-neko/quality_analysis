# quality_analysis

Lightweight **Java 8** JDBC data-quality profiler and validation engine. Current target tables are mostly around 200k rows and below 1M rows.

The database is only the streaming data source; profiling and configured quality rules run locally in Java so the same engine can later be reused across DM, Oracle, MySQL, PostgreSQL, etc.

## Current scope

- Java 8; zero third-party production runtime dependencies
- `TYPE_FORWARD_ONLY` + `CONCUR_READ_ONLY` + configurable JDBC `fetchSize` (default 10000)
- no application paging, OFFSET/LIMIT, or `ORDER BY` primary key
- best-effort `DatabaseMetaData.getPrimaryKeys()`; PK is a label, never a scan prerequisite
- rows are consumed immediately; no application-side `List<10000 rows>`
- CLOB/BLOB have explicit bounded behavior
- profiling and configured rules share **one JDBC scan**
- deterministic mock data is the permanent regression baseline

## Minimal profile is the default

`ProfileOptions.defaults()` / `ProfileOptions.minimal()` intentionally keep only high-value signals enabled:

| Capability | Default |
|---|---|
| physical NULL / blank / semantic-null counts | ON |
| exact distinct / uniqueness | ON |
| low-cardinality value frequencies | ON |
| `distinct <= 20` full enum values | ON |
| numeric/date min-max | ON |
| string/LOB length statistics | ON |
| format Pattern profile (`D11`, `L2D9`...) | OFF |
| character-shape statistics | OFF |
| case-variant grouping | OFF |
| relationship fingerprint / MinHash | OFF |
| CLOB text preview | OFF (`clobPreviewChars=0`) |
| BLOB content loading | OFF by design |

Optional profiling is explicitly enabled only when needed:

```java
ProfileOptions options = ProfileOptions.builder()
        .fetchSize(10_000)
        .patternProfileEnabled(true)
        .relationshipFingerprintEnabled(true)
        .build();
```

Expensive/high-cardinality work can also be disabled explicitly, for example:

```java
ProfileOptions options = ProfileOptions.builder()
        .distinctEnabled(false)
        .valueFrequencyEnabled(false)
        .rangeEnabled(true)
        .lengthEnabled(true)
        .build();
```

`valueFrequencyEnabled(true)` and `relationshipFingerprintEnabled(true)` require exact distinct to be enabled because they reuse the same distinct state rather than creating another set.

## Data flow

```text
DM / JDBC database
      |
      | SELECT * / forward-only ResultSet / fetchSize
      v
JdbcTableProfiler
      |-- ResultSetMetaData -> typed ColumnMetadata
      |-- DatabaseMetaData.getPrimaryKeys() -> optional PK label
      v
one raw cell
      |-----------------------------|
      v                             v
ProfileEngine                    RuleEngine
actual data shape               configured expectation
      |                             |
      |-----------------------------|
                    v
              AnalysisResult
          TableProfile + RuleResult[]
```

JDBC/DM handles fetch buffering internally. Fetch boundaries never define profile boundaries; accumulator state survives until the complete table scan finishes.

## Built-in configured rules

V1 contains five deliberately small rules:

| Rule | Meaning |
|---|---|
| `NullRateRule` | maximum physical/blank/semantic-null rate |
| `DictionaryRule` | every non-missing value must belong to a configured set |
| `RangeRule` | inclusive NUMBER or DATE_TIME range |
| `RegexRule` | every non-missing normalized value must fully match one Java regex |
| `UniqueRule` | minimum uniqueness; reuses the profiler distinct accumulator |

Example:

```java
List<RuleBinding> rules = Arrays.asList(
        new RuleBinding("CUSTOMER_ID", new UniqueRule("customer-id-unique")),
        new RuleBinding("GENDER", new DictionaryRule(
                "gender-dict", Arrays.asList("M", "F", "U"))),
        new RuleBinding("AGE", new RangeRule("age-range", "0", "120")),
        new RuleBinding("PHONE", new RegexRule(
                "phone-format", "^1[3-9]\\d{9}$")),
        new RuleBinding("OPTIONAL_CODE", new NullRateRule(
                "optional-null-rate", 0.05d))
);

AnalysisResult result = new QualityAnalyzer().analyzeTable(
        connection, "SCHEMA_NAME", "CUSTOMER",
        ProfileOptions.defaults(), rules);
```

A regex is a **format rule only**. For example `^\\d{17}[0-9Xx]$` can check the shape of a Chinese ID number, but checksum, birth-date and administrative-code validation should be implemented as a custom rule rather than hidden inside the regex rule.

Rules skip missing values when the rule has another responsibility (Regex/Dictionary/Range). Missing-value policy belongs to `NullRateRule`, preventing the same missing cell from being reported as several unrelated failures.

Rule results contain the column, checked/invalid counts, valid/invalid rate, a short message and at most a bounded number of invalid samples.

## DM / 达梦

The main module does not compile against DM-specific classes. Put the DM JDBC jar on the runtime classpath.

Recommended behavior:

- `clobAsString=false` (DM default): keep CLOB detectable as a LOB rather than silently turning it into VARCHAR.
- `LobMode=1` (DM default): LOB content is fetched on demand rather than fully cached with the result set.
- `Statement.setFetchSize(...)` is used for driver/database result prefetch behavior.
- PK labels are read through standard `DatabaseMetaData.getPrimaryKeys()` when available.

### LOB policy

| type | behavior |
|---|---|
| CLOB / NCLOB | length only by default; optional bounded prefix preview |
| BLOB | length only; bytes are never loaded |
| LONGVARCHAR | CLOB strategy if the driver returns `Clob`, otherwise normal string handling |
| normal CHAR/VARCHAR | normal profile/rules |

LOB content never participates in distinct/uniqueness. A CLOB preview is informational only and never becomes a fake distinct key.

## Low-cardinality enum discovery

With default options:

- final `distinct <= 20`: emit every retained value with exact frequency and ratio
- `20 < distinct <= 100`: retain low-cardinality values and emit Top N
- above the configured low-cardinality limit: discard the frequency map to protect memory while exact distinct can continue

This makes fields such as status/gender/type immediately useful even before a formal data dictionary has been configured.

## Optional relationship discovery

Relationship discovery is **off by default**. When `relationshipFingerprintEnabled(true)` is enabled, each newly observed distinct value updates:

```text
distinctCount
hashSum64
hashXor64
secondHashSum64
one-permutation MinHash bins
```

Matching low-cardinality sets can become `SHARED_DICTIONARY` candidates; higher-cardinality exact matches become `SAME_VALUE_DOMAIN`; compatible approximate overlaps may become `POTENTIAL_RELATION`.

These are discovery signals, not proof of a database constraint.

## Build and regression

Requirements: JDK 8+ and Maven 3.x.

```bash
mvn clean test
mvn clean package
```

GitHub Actions runs the same Java 8 Maven regression suite.

When Maven is unavailable, a JDK-only regression runner is included:

```bash
bash scripts/manual-regression.sh
```

Production code has zero external runtime dependencies. JUnit 4 is test scope only.

## DM runtime example

Put the DM JDBC jar outside the repository and include both jars on the classpath. Windows uses `;` as the classpath separator.

```text
java -cp "target/quality-analysis-0.1.0-SNAPSHOT.jar;D:\\path\\DmJdbcDriver18.jar" ^
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli ^
  dm.jdbc.driver.DmDriver ^
  jdbc:dm://127.0.0.1:5236/DAMENG ^
  USER PASSWORD SCHEMA TABLE 10000
```

The CLI currently prints the automatic profile. Configured-rule execution is exposed through the Java API first; a rule-config file/CLI can be added later when its format is stable.

## Mock regression baseline

The permanent mock suite covers candidate keys, enums, whitespace/case pollution, physical/blank/semantic nulls, numeric outliers, phone regex anomalies, date outliers, constants, shared dictionaries, CLOB/BLOB behavior and all five built-in configured rules.

The policy is simple: every feature or bug fix adds deterministic mock data + assertions before real-DM verification.

## Extension and design docs

- `docs/DESIGN.md` — architecture and boundaries
- `docs/RULES.md` — built-in rules and how to add a custom rule
- `docs/MOCK_TESTSET.md` — permanent mock/regression contract

Deferred until real measurements justify them: HLL/approximate distinct, sampling, parallel full-table scanning, database-side rule pushdown, persistent profile cache and complex domain-specific validators.
