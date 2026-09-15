# quality_analysis

Lightweight Java 8 JDBC data profiling toolkit. V1 focuses on **profile only**: read table data once, calculate useful field/table profiles, persist each completed table immediately, then generate Excel/HTML reports from the saved records.

The database is only the streaming data source. Profiling runs locally in Java so the same core can later be reused across DM, Oracle, MySQL, PostgreSQL, etc.

## V1 scope

V1 deliberately contains **no configured Rule engine**. Rules need field-specific business configuration; that is not required for the first delivery.

The rule-capable implementation is preserved in branch:

```text
archive/profile-with-rules
```

Do not re-introduce Rule classes into V1 unless the product scope changes.

## Scan + persistence model

```text
DM / JDBC database
      |
      | SELECT *
      | TYPE_FORWARD_ONLY + CONCUR_READ_ONLY
      | Statement.setFetchSize(...)
      v
JdbcTableProfiler
      |
      v
ProfileEngine
      |
      v
TableProfile
      |
      | one table completed -> persist immediately
      v
runs/<runId>/tables/<schema>.<table>.json
      |
      +--> quality-profile.xlsx
      `--> quality-profile.html
```

There is no application-side paging, OFFSET/LIMIT paging, or `ORDER BY` primary key. JDBC/DM handles fetch buffering; rows are consumed immediately and are not accumulated into a `List<10000 rows>`.

Primary-key metadata is read best-effort through `DatabaseMetaData.getPrimaryKeys()`. PK is only a metadata label and is never a scan prerequisite.

## Durable run directory

Each scan run is self-contained:

```text
runs/20260915_001/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   ├── TEST.ORDERS.json
│   └── ...
├── quality-profile.xlsx
└── quality-profile.html
```

- `manifest.json`: run id, database label, start/end time, success/failure counts and ProfileOptions snapshot. It never stores passwords.
- `tables/*.json`: one completed table = one durable record.
- JSON is written to `*.tmp` first and then replaced with an atomic move when supported.
- A failed later table does not destroy earlier successful table results.
- A single table record can be overwritten by a later rerun without rewriting every other table.
- Excel/HTML read these persisted records; they do not rescan the database and do not recalculate profile metrics.

V1 intentionally uses files instead of SQLite or another service database: deployment stays simple, results are portable, human-readable, and easy to archive. A database-backed repository can be added later if multi-user/history requirements justify it.

## Minimal Profile defaults

Default profiling keeps only high-value information:

- row count
- physical NULL count/rate
- blank count
- semantic-null count (`NULL`, `N/A`, `NA` by default)
- exact distinct count
- uniqueness ratio
- low-cardinality values/frequencies
- `distinct <= 20`: emit all distinct values with counts/ratios
- numeric/date min and max
- string/LOB length statistics
- declared primary-key label
- candidate unique-key flag
- constant / quasi-constant / potential-enum flags

More exploratory capabilities are optional and OFF by default:

- format Pattern profile, e.g. `D11`, `L2D9`, `D4-D2-D2`
- string-shape statistics
- case-variant discovery
- set fingerprint / MinHash relationship discovery
- CLOB text preview

Use `ProfileOptions` to enable only what is needed.

## CLOB / BLOB

LOB values are handled separately so a full scan does not accidentally materialize large content.

| Type | V1 behavior |
|---|---|
| CLOB / NCLOB | read `Clob.length()`; content skipped by default; optional bounded preview |
| BLOB | read `Blob.length()`; bytes are not loaded |
| normal VARCHAR/CHAR | normal profile |

LOB content does not participate in distinct/uniqueness.

For DM, keep `clobAsString=false` when possible so CLOB stays identifiable as CLOB. The DM JDBC driver jar remains an external runtime dependency and is not committed to the repository.

## Reports

### Excel

`quality-profile.xlsx` currently contains:

1. `扫描概览` — run metadata and aggregate counts.
2. `字段质量明细` — the main delivery sheet, one row per field.
3. `探查提示` — profile insights such as potential enum, candidate unique key, constant/quasi-constant fields.

The field detail sheet includes database/schema/table/column, DB/JDBC type, PK label, row count, NULL/blank/semantic-null, distinct, uniqueness, min/max, length statistics, enum/TopN values and profile insight flags.

### HTML

`quality-profile.html` is a self-contained static file. It provides:

- overall scan summary
- searchable table directory
- table overview
- one section per table
- field-level profile metrics and discovery tags

No server, Spring, Node.js, Nginx or browser plugin is required; open the file directly.

V1 reports do **not** show Rule PASS/FAIL or quality scores. Profile describes facts; it does not invent business correctness.

## Main models

```text
ColumnMetadata      = field definition / JDBC metadata
ColumnProfile       = actual profile of one field
TableMetadata       = table + column definitions
TableProfile        = in-memory result of one table scan
TableProfileRecord  = stable persisted/report representation
RunManifest         = durable run-level progress and metadata
```

## Build

Requirements: JDK 8+ and Maven 3.x.

```bash
mvn clean test
mvn clean package
```

Runtime dependencies are intentionally limited to:

- Gson 2.10.1 for stable JSON persistence
- Apache POI 5.2.2 for `.xlsx` output

The profile core remains standard JDBC/Java code. `scripts/manual-regression.sh` still runs the dependency-free core smoke regression when Maven is unavailable.

## DM report-mode example

Put the DM JDBC jar outside the repository and include it on the runtime classpath. Windows uses `;` as the separator.

```text
java -cp "target/quality-analysis-0.1.0-SNAPSHOT.jar;D:\path\DmJdbcDriver18.jar;target\dependency\*" ^
  com.initialneko.qualityanalysis.cli.QualityAnalysisCli ^
  dm.jdbc.driver.DmDriver ^
  jdbc:dm://127.0.0.1:5236/DAMENG ^
  USER PASSWORD DM_TEST SCHEMA TABLE1,TABLE2 D:\quality-runs 10000
```

Arguments in report mode:

```text
<driver-class> <jdbc-url> <user> <password> <database-label> <schema>
<table1,table2,...> <output-root> [fetchSize]
```

Legacy single-table console mode remains available:

```text
<driver-class> <jdbc-url> <user> <password> <schema> <table> [fetchSize]
```

## Mock regression baseline

`MockDatasets` is the permanent development test dataset. New profiling/report features should first add deterministic mock data + assertions. Current report regression verifies JSON persistence/read-back, workbook sheets/key cells, and static HTML content.

## Branches

```text
main                         preserved merged capability baseline
archive/profile-with-rules   complete Profile + Rule implementation (retained)
feature/profile-only-v1      active V1: Profile + persistence + reports
```

See `docs/DESIGN.md`, `docs/MOCK_TESTSET.md`, and `docs/REPORTS.md`.
