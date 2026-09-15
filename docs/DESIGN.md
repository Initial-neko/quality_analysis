# V1 Design — Profile Only

## Scope

V1 is intentionally profile-only. It does not contain configured field rules, rule bindings, validation statuses, or rule-result models.

The complete Profile + Rule implementation is preserved in:

```text
archive/profile-with-rules
```

The active V1 branch is:

```text
feature/profile-only-v1
```

## Constraints

- Java 8
- minimal dependencies
- DM is the first database
- most tables are around 200k rows; current ceiling is roughly 1M rows
- one table scan should produce all enabled profile metrics
- no application paging
- no primary-key ordering requirement
- LOB content must not be materialized by default
- optional exploratory metrics must be switchable
- one completed table must be persisted immediately
- reports must read persisted records rather than rescan the source database

## End-to-end path

`JdbcTableProfiler` performs one forward-only scan per table. `ProfileRunService` executes tables sequentially and persists each completed result before starting the next table.

```text
Database
   |
ResultSet (forward only + fetchSize)
   |
ProfileEngine
   |
TableProfile
   |
TableProfileRecord
   |
JSON per table + RunManifest
   |
   +-- ExcelProfileReportWriter
   `-- HtmlProfileReportWriter
```

Fetch boundaries are JDBC/driver transport details; they are not profile boundaries.

## JDBC scan path

1. best-effort `DatabaseMetaData.getPrimaryKeys()`
2. `SELECT * FROM schema.table`
3. `TYPE_FORWARD_ONLY` + `CONCUR_READ_ONLY`
4. configurable `Statement.setFetchSize(...)`
5. `ResultSetMetaData` -> `TableMetadata` / `ColumnMetadata`
6. every cell is consumed once by `ProfileEngine`
7. final in-memory table output is `TableProfile`
8. the completed table is converted to `TableProfileRecord` and written immediately

There is no OFFSET/LIMIT paging and no `ORDER BY` primary key requirement.

## Core models

### ColumnMetadata

Physical field metadata:

- column name / label
- JDBC type
- database-native type name
- precision / scale
- nullable
- declared-primary-key label
- normalized `ValueFamily`

### ColumnProfile

Observed field profile. It answers “what does the data currently look like?” rather than “is the data valid?”.

Core metrics include:

- row / non-null / null counts
- blank and semantic-null counts
- exact distinct count
- uniqueness
- low-cardinality values/frequencies
- min/max
- min/max/average length
- candidate key
- constant / quasi-constant
- potential enum

Optional fields may include:

- pattern fingerprints
- string-shape statistics
- case-variant groups
- set fingerprint / MinHash sketch

### TableProfile

One table scan result used inside the profiling layer:

```text
TableMetadata
rowCount
List<ColumnProfile>
```

### TableProfileRecord

Stable persisted/report DTO. It contains the report-facing subset of table/column metadata and observed metrics. Reports consume this model so report concerns do not leak into the profiling core.

### RunManifest

Durable run-level metadata:

- run id
- non-sensitive database label
- start/end time and status
- planned/success/failed table counts
- ProfileOptions snapshot
- one execution entry per attempted table

Credentials are never stored.

## Persistence boundary

V1 uses one JSON file per completed table:

```text
<run>/tables/<schema>.<table>.json
```

and one run manifest:

```text
<run>/manifest.json
```

Files are written through `*.tmp` and replaced atomically where the filesystem supports it. This protects already-completed tables when a later scan fails or the process is interrupted.

V1 does not use JSONL because the execution/retry unit is a table and individual replacement matters. It does not use SQLite because there is no current history/query/multi-user requirement that justifies database deployment and schema migration.

## Exact distinct

At the current 200k–1M scale, V1 uses exact `HashSet` distinct tracking for enabled columns. This keeps results deterministic and makes uniqueness / enum discovery straightforward.

Memory control principles:

- process tables sequentially by default
- persist a completed table before scanning the next one
- release accumulators after a table result is written
- do not distinct-profile LOB content
- low-cardinality frequency maps stop growing after their configured limit
- optional relationship sketches can be disabled

Do not add HLL until real measurements justify it.

## Low-cardinality behavior

Default behavior:

```text
distinct <= 20      emit all values + count + ratio
20 < distinct <= N  retain bounded low-cardinality frequencies / Top N
high cardinality    do not retain all frequencies
```

A low-cardinality field is only a discovery signal (e.g. potential enum). It is not a quality failure.

## LOB behavior

CLOB/NCLOB:

- use `Clob.length()`
- skip content by default
- optional bounded prefix preview only when explicitly enabled

BLOB:

- use `Blob.length()`
- never materialize bytes for profiling

LOB content does not participate in distinct/uniqueness.

## Optional exploration

The following are not required for the minimal V1 result and are disabled by default:

- pattern fingerprint
- character-shape statistics
- case variants
- relationship fingerprint / MinHash
- CLOB preview

They remain useful for ad-hoc exploration, but report V1 must not depend on them.

## Reporting boundary

Report generation is downstream from persistence:

```text
manifest.json + tables/*.json
        |
        +-- quality-profile.xlsx
        `-- quality-profile.html
```

Writers only format/aggregate saved profile facts. They do not query the source database, calculate a second copy of field metrics, or invent validation statuses.

Excel V1:

- scan overview
- field detail: one row per database/schema/table/field
- profile discovery/enum insights

HTML V1:

- overall run overview
- searchable table directory
- table overview
- per-table field details

Apache POI 5.2.2 is used only for XLSX rendering. Gson is used only for JSON persistence. The profiling core remains standard Java/JDBC.

## Execution service

`ProfileRunService` owns the sequence:

1. create run directory and RUNNING manifest;
2. scan one table;
3. persist its `TableProfileRecord`;
4. update manifest immediately;
5. continue to the next table;
6. finish manifest;
7. generate Excel and HTML from persisted records.

The current implementation continues after a table-level `SQLException`. If every table fails, the first SQL exception is rethrown after the durable run metadata/report attempt.

## Deferred

Do not add these to V1 unless the scope changes:

- configured Rule engine
- dictionary/range/regex validation
- PASS/WARN/FAIL quality scoring
- HLL/approximate distinct
- sampling
- broad parallel scans
- DB-side pushdown
- historical trend database
- remediation workflow

The previous configured-rule implementation remains available in `archive/profile-with-rules` for future reuse.
