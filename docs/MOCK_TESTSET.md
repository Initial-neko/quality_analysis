# Mock regression dataset

The mock dataset is the permanent development baseline for the **profile-only V1**. New profiling capabilities must add deterministic scenarios and assertions before they are connected to a real DM database.

Configured rules are outside V1 scope. Rule-specific regression cases are retained only in `archive/profile-with-rules`.

## CUSTOMER (200 rows)

| Column | Scenario / expected profile value |
|---|---|
| CUSTOMER_ID | 1..200; exact unique candidate key and declared-PK label |
| STATUS | ACTIVE / INACTIVE / PENDING plus ` active ` and `Active`; low-cardinality enum-like field |
| GENDER | M / F / U; shared with ORDERS for optional relationship discovery |
| AGE | normal values plus -1 and 150; profile should expose min/max only, not call them invalid |
| PHONE | mostly 11 digits, one `138-1234-5678`; optional pattern profile can surface the format difference |
| CREATE_DATE | mostly 2026-09-15, one 2099-12-31; profile exposes range only |
| SOURCE_SYSTEM | constant CRM |
| NOTE | mock CLOB descriptors, length 101..300 |
| PAYLOAD | mock BLOB descriptors, length 2049..2248 |
| OPTIONAL_CODE | physical null, blank, semantic `NULL`, then a small code set |

## ORDERS (120 rows)

| Column | Scenario |
|---|---|
| ORDER_ID | unique candidate key |
| CUSTOMER_ID | subset 1..120 of CUSTOMER ids; optional future relationship-discovery input |
| GENDER | exactly M / F / U; optional shared-dictionary discovery |
| ORDER_STATUS | small enum-like profile |

## What V1 core tests should assert

- stable row counts
- physical NULL / blank / semantic-null counts
- exact distinct counts
- uniqueness ratios / candidate-key flags
- low-cardinality emitted values
- numeric/date min/max
- length statistics
- LOB content is not materialized
- Minimal Profile optional switches are OFF by default
- optional Pattern / relationship features work only when explicitly enabled

Do **not** assert PASS/FAIL validity in V1. For example, AGE=-1 and AGE=150 are deliberately present so min/max can be tested; whether those values are valid requires a future field-specific rule.

## Persistence/report regression

`ProfilePersistenceAndReportTest` treats the persisted format as part of the V1 contract:

1. CUSTOMER is converted to `TableProfileRecord` and written as one JSON table record;
2. the record is read back and key metrics such as row count, STATUS distinct count and candidate-key flag are asserted;
3. manifest progress is written/read independently;
4. CUSTOMER + ORDERS persisted records are used to generate `quality-profile.xlsx` and `quality-profile.html`;
5. the workbook must contain `扫描概览`, `字段质量明细`, `探查提示` and key table/field values;
6. the HTML must contain overall report text plus CUSTOMER/PHONE/profile insight content.

The report tests deliberately generate reports **after re-reading persisted records**. This enforces the architecture that report writers do not query the source database and do not recalculate field metrics from raw rows.

## Regression policy

- deterministic data only; do not replace scenarios with random generation
- add a deterministic case for each bug fix
- expected profile metrics should be explicit
- real-DM integration tests are a separate layer and must not replace the in-memory suite
- report writers consume persisted `TableProfileRecord` data, not raw JDBC rows
