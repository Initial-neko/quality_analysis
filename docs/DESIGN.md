# Design

## Constraints

- Java 8, minimal dependencies.
- Most tables are around 200k rows; current ceiling is roughly 1M rows.
- DM is the first database, but scanning/profiling/rules remain standard-JDBC based.
- A profile must be useful before business rules exist.
- Optional discovery must not become mandatory CPU/memory work.

## One database scan

`JdbcTableProfiler` performs one forward scan:

1. best-effort `DatabaseMetaData.getPrimaryKeys()`
2. `SELECT * FROM schema.table`
3. `TYPE_FORWARD_ONLY` + `CONCUR_READ_ONLY`
4. configurable `Statement.setFetchSize(...)`
5. typed metadata from `ResultSetMetaData`
6. read each cell once through `JdbcValueReader`
7. send that same value to both `ProfileEngine` and `RuleEngine`
8. finish profile, then let profile-aware rules (notably `UniqueRule`) reuse the result

There is no OFFSET/LIMIT/keyset paging and no PK ordering. PK metadata is only a label.

## Typed values and LOBs

Normal JDBC values stay typed. Canonical strings are created only where a set/string operation needs them. Numeric/date comparisons use typed values, avoiding lexical errors such as `100 < 2`.

CLOB/BLOB are converted to lightweight `LobValue` descriptors. Default CLOB behavior is length only; an optional prefix preview is bounded. BLOB bytes are never materialized. LOB previews never participate in distinct/uniqueness.

## Minimal profile

The default profile keeps only signals with high value/cost ratio:

- physical null / blank / semantic-null counts
- exact distinct + uniqueness
- low-cardinality value frequencies and potential enum values
- numeric/date min/max
- string/LOB lengths
- declared PK + candidate unique-key signal
- constant/quasi-constant signal

Optional and disabled by default:

- format Pattern fingerprints
- character-shape statistics
- case-variant grouping
- set fingerprint / MinHash relationship discovery
- CLOB text preview

Every optional feature has an explicit switch in `ProfileOptions`.

## Memory behavior

Rows are not retained. Accumulator state survives across JDBC fetch boundaries.

Exact distinct uses one `HashSet<String>` per enabled column. `UniqueRule` deliberately reuses this profile state and never creates another distinct set. Low-cardinality frequency maps are discarded after their configured limit is exceeded.

At the current 200k~1M scale this is preferred over introducing HLL/approximate cardinality before measurements require it.

## Rule layer

A configured rule is bound to one column through `RuleBinding`.

`ColumnRule` lifecycle:

```text
start(ColumnMetadata, ProfileOptions)
        |
accept(rawValue)  <- zero or more values from the same JDBC scan
        |
finish(ColumnProfile)
        |
RuleResult
```

This supports two kinds of rules without separate architectures:

- streaming rules: Regex, Dictionary, Range, custom business logic
- profile-backed rules: Unique, or future thresholds that can reuse aggregate profile state

Built-ins are intentionally narrow: `NullRateRule`, `DictionaryRule`, `RangeRule`, `RegexRule`, `UniqueRule`.

Missing-value policy is separated from semantic rules: Regex/Dictionary/Range skip missing values; `NullRateRule` owns missing-value validation.

## Relationship discovery

Relationship discovery is optional. When enabled, the exact-distinct first-seen event also updates three 64-bit set aggregates and one-permutation MinHash bins. No second set is created.

Candidates are hints only:

- `SHARED_DICTIONARY`
- `SAME_VALUE_DOMAIN`
- `POTENTIAL_RELATION`

## Extensibility rule

Do not add a new framework for a new validation. Prefer implementing `ColumnRule` (or extending `AbstractColumnRule`) and binding it through `RuleBinding`. A custom rule should:

- keep bounded state
- never retain complete rows
- never issue its own database query
- bound invalid samples
- reuse `ColumnProfile` where an aggregate already exists
- add deterministic mock data and regression assertions

See `RULES.md` for the implementation template.

## Deferred features

Do not add HLL, sampling, broad parallel scans, database-side rule pushdown, persistent caches, complex ID-card/domain validators, or a generic rule DSL until a real requirement/measurement justifies them.
