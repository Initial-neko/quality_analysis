# Mock regression dataset

The mock dataset is the permanent development test set. New profiling/rule capabilities add deterministic scenarios and assertions before being connected to DM.

## CUSTOMER (200 rows)

| Column | Scenario |
|---|---|
| CUSTOMER_ID | 1..200, exact unique candidate key and declared-PK label |
| STATUS | ACTIVE / INACTIVE / PENDING plus ` active ` and `Active` pollution |
| GENDER | M / F / U; reused by ORDERS for shared-dictionary discovery |
| AGE | normal values plus -1 and 150 |
| PHONE | mostly 11 digits, one `138-1234-5678` format anomaly |
| CREATE_DATE | mostly 2026-09-15, one 2099-12-31 outlier |
| SOURCE_SYSTEM | constant CRM |
| NOTE | mock CLOB descriptors, length 101..300 |
| PAYLOAD | mock BLOB descriptors, length 2049..2248 |
| OPTIONAL_CODE | physical null, blank, semantic `NULL`, then a small code set |

The CUSTOMER/ORDERS fixtures explicitly enable Pattern/case-variant/relationship features because they test optional discovery. This does not change the minimal production defaults.

## ORDERS (120 rows)

| Column | Scenario |
|---|---|
| ORDER_ID | unique key |
| CUSTOMER_ID | subset 1..120 of CUSTOMER ids; future containment/FK test source |
| GENDER | exactly M / F / U, shared dictionary candidate |
| ORDER_STATUS | small enum |

## Built-in rule fixture (5 rows)

`RuleEngineTest` uses `ID / GENDER / AGE / PHONE / OPTIONAL_CODE` and intentionally injects exactly one failure for each semantic rule plus two missing values:

| Rule | Injected condition | Expected invalid |
|---|---|---:|
| UniqueRule | duplicated ID `2` | 1 |
| DictionaryRule | GENDER=`X` outside M/F/U | 1 |
| RangeRule | AGE=`130` outside 0..120 | 1 |
| RegexRule | PHONE=`138-0000-0003` | 1 |
| NullRateRule | one physical NULL + one semantic `NULL` | 2 |

The Regex test also asserts that the invalid sample is retained in bounded result samples.

## Minimal-default fixture

`MinimalProfileOptionsTest` confirms that defaults still calculate exact distinct, low-cardinality values, range and lengths while leaving Pattern, case-variant grouping and relationship MinHash disabled.

## Regression policy

- Do not replace deterministic scenarios with random data.
- Add a deterministic case for each bug fix and new rule.
- Keep expected results explicit in tests.
- Optional features must be explicitly enabled by the tests that need them.
- Real-DM integration tests are a separate layer and must not replace the in-memory suite.
