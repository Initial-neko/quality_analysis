# Profile persistence and reports

## Purpose

V1 scans tables sequentially. A completed table must become durable immediately; later tables must not be able to erase earlier results. Reports are generated from persisted records only.

This keeps three concerns separate:

```text
JDBC scan -> Profile -> persistence -> report rendering
```

Report rendering never queries the source database.

## Why one JSON file per table

V1 stores one `TableProfileRecord` per table instead of using JSONL, Java serialization or SQLite.

### Compared with JSONL

JSONL is convenient for append-only events, but replacing one table after a rerun requires rewriting or de-duplicating the whole file. One file per table maps directly to the execution unit and makes overwrite/recovery simple.

### Compared with Java serialization

Java serialization is opaque and tightly coupled to class versions. JSON records are readable, diffable and can later be consumed by other tools.

### Compared with SQLite

SQLite would be reasonable when the product needs historical queries, multiple users, cross-run comparison or concurrent writers. V1 only needs durable local output and portable delivery, so a database adds deployment and schema-migration cost without enough benefit.

## Run directory

```text
<output-root>/<runId>/
├── manifest.json
├── tables/
│   ├── TEST.CUSTOMER.json
│   ├── TEST.ORDERS.json
│   └── ...
├── quality-profile.xlsx
└── quality-profile.html
```

`runId` uses a timestamp with milliseconds. Credentials are never written to the run directory.

## manifest.json

`RunManifest` records:

- run id
- non-sensitive database label
- start/end time
- RUNNING / COMPLETED / COMPLETED_WITH_ERRORS
- planned/success/failed table counts
- key `ProfileOptions`
- one execution entry per attempted table

The manifest is rewritten after every table using temp + atomic move when supported. If the process stops after table 17, the manifest and the first 17 table records remain usable.

## TableProfileRecord

The persisted record is intentionally separate from `TableProfile`.

`TableProfile` is the profiling engine model. `TableProfileRecord` is a stable persistence/report DTO. This prevents report/file concerns from leaking into the profiling core.

A record contains:

- database / schema / table
- scan timestamp / duration / row count
- column JDBC/database metadata
- NULL / blank / semantic-null metrics
- distinct / uniqueness
- min / max
- string/LOB lengths
- low-cardinality values + exact frequencies retained by the profiler
- potential-enum / candidate-key / constant / quasi-constant flags
- optional pattern/string-shape output when those profile switches were enabled

A table record is written to `*.tmp` first and then replaces the target JSON. This avoids leaving a half-written normal record after interruption.

## Excel report

`quality-profile.xlsx` has three sheets.

### 扫描概览

Run metadata and aggregate counts:

- planned/success/failed tables
- saved table records
- total fields
- cumulative rows scanned
- potential enum fields
- candidate unique keys
- constant fields
- fetchSize

### 字段质量明细

Main delivery sheet. One field per row.

Columns include:

- database / schema / table / field / label
- database type / JDBC type / normalized family
- nullable / declared PK
- row count
- NULL / NULL rate / blank / semantic null / non-null
- distinct / uniqueness
- min / max
- min / max / average length
- potential enum + retained enum/TopN values
- candidate unique key
- constant / quasi-constant
- LOB-content-skipped flag

This sheet is intended for filtering, sorting and governance follow-up.

### 探查提示

Only profile-derived discovery signals are listed:

- potential enum
- candidate unique key
- constant field
- quasi-constant field

These are observations, not validation failures.

## HTML report

`quality-profile.html` is self-contained and contains inline CSS/JavaScript. It can be opened directly from disk.

It provides:

- overall summary cards
- run metadata
- searchable table directory
- table overview
- per-table field profile table
- discovery tags and enum/TopN values

No server-side runtime is required.

## Important semantic boundary

V1 does not contain field-specific Rules.

For example:

```text
AGE min=-1 max=150
```

is reported as an observed range. V1 does not call `-1` invalid because no business range rule has been configured.

Likewise:

```text
distinct=5
```

may produce a `potential enum` insight, but it is not a PASS/FAIL result.

The preserved future rule implementation remains in:

```text
archive/profile-with-rules
```

## Re-running one table

A single-table rerun can write the same `<schema>.<table>.json` file again. Report generation then reads the latest saved record. V1 does not yet implement a UI for resume/retry selection; the storage design deliberately makes that future feature straightforward.

## Tests

`ProfilePersistenceAndReportTest` verifies:

1. one table can be persisted and loaded back;
2. manifest progress survives independently;
3. reports are generated from saved records;
4. the XLSX contains the expected sheets and key fields;
5. the HTML contains the expected table/field/profile content.
