# Full 4-way benchmark: data.json, cheshire, jsonista 1.0.0 (Jackson 2), jsonista 2.0.0 (Jackson 3 + decoder optimizations)

**Date:** 2026-08-01
**Runs:**
- Jackson 2: `master` (jsonista 1.0.0, Jackson 2.21.2) in a worktree, with the branch's benchmark harness applied — `2026-07-31-full-jackson2.txt`
- Jackson 3: `jackson-3-port` (Jackson 3.2.1) at the decoder-optimization commits — `2026-08-01-full-jackson321.txt`

## Method

Both jsonista versions define the same namespaces and Java class names, so they
cannot coexist in one JVM. The comparison is therefore two sequential runs of an
identical harness on the same quiesced machine (Apple Silicon MacBook Pro,
OpenJDK 25.0.2 Corretto), with data.json and cheshire — untouched by either
branch — as cross-run drift controls.

Config: 1 fork, warmup 3×3 s, measurement 5×3 s, per benchmark point.
The master worktree's `jmh.clj` got the same shadowing fix as the branch (its
`:encode`/`:decode` groups previously measured a tagged-module mapper, see
`2026-07-31-comparison.md` §5), so both runs measure stock jsonista.

### Control drift (Jackson 3 run vs Jackson 2 run, identical code)

| control | 10b | 100b | 1k | 10k | 100k |
|---|---:|---:|---:|---:|---:|
| encode-data-json | −18.6% | +0.5% | +1.2% | +0.3% | −4.7% |
| decode-data-json | +12.0% | −5.6% | +6.1% | −0.2% | −6.2% |
| encode-cheshire | +11.0% | −1.0% | −1.0% | +1.0% | −4.9% |
| decode-cheshire | +4.1% | +5.5% | +5.9% | −4.5% | +4.1% |
| encode-cheshire-kw | −6.9% | −4.7% | −1.1% | +0.2% | −3.0% |
| decode-cheshire-kw | +3.6% | +2.1% | +5.0% | +3.3% | +5.4% |

At 100 b and above the floor is ~±6%. At 10 b it is ±12–19% — deltas below
that magnitude at 10 b are not interpretable.

## jsonista: 1.0.0 (Jackson 2) → 2.0.0 (Jackson 3 + optimizations)

| benchmark | 10b | 100b | 1k | 10k | 100k |
|---|---:|---:|---:|---:|---:|
| decode (string keys) | +17.3% | **+21.7%** | **+22.8%** | **+17.4%** | **+15.4%** |
| decode (keyword keys) | +8.2% | **+48.0%** | **+27.9%** | **+32.0%** | **+29.1%** |
| encode (string keys) | −28.6% | −12.7% | −0.3% | −2.1% | −1.8% |
| encode (keyword keys) | −46.5% | −9.5% | −3.1% | +1.9% | +5.9% |
| encode (keyword, bytes) | −47.0% | −7.6% | −2.2% | +1.0% | −1.6% |
| *raw Jackson encode (engine)* | *−45.5%* | *−10.7%* | *−7.7%* | *+0.2%* | *+4.3%* |
| *raw Jackson decode (engine)* | *−28.0%* | *−8.9%* | *−3.5%* | *−0.2%* | *−0.9%* |

Bold: outside the control noise floor for that size.

**Decode is 15–23% faster with string keys and 28–48% faster with keyword
keys at 100 b+** — and this is *despite* the Jackson 3 engine itself decoding
somewhat slower than Jackson 2. The gains come from the decoder work on this
branch (keyword intern cache, array maps for small objects, single-pass
untyped deserializer), not the engine.

**Encode is unchanged at 1 kB+.** The large negative encode deltas at
10 b–100 b track the raw Jackson engine rows almost exactly (jsonista −28.6%
where raw Jackson is −45.5% at 10 b): this is the Jackson 3 small-payload
encode regression already flagged in `2026-07-31-comparison.md`, present with
no jsonista code in the loop. Nothing to fix on the jsonista side.

## Jackson 3 run, absolute (ops/s)

### encode, string keys

| size | data.json | cheshire | jsonista | Jackson (raw) |
|---|---:|---:|---:|---:|
| 10b | 5,917,892 | 2,519,476 | 9,287,998 | 9,376,698 |
| 100b | 1,421,856 | 1,541,245 | 3,481,862 | 3,523,568 |
| 1k | 210,328 | 471,888 | 716,087 | 655,584 |
| 10k | 16,047 | 44,068 | 57,715 | 55,877 |
| 100k | 1,647 | 4,253 | 5,528 | 5,534 |

### decode, string keys

| size | data.json | cheshire | jsonista | Jackson (raw) |
|---|---:|---:|---:|---:|
| 10b | 8,481,497 | 2,189,307 | 9,031,389 | 8,448,628 |
| 100b | 2,108,504 | 1,260,362 | 2,411,301 | 2,406,312 |
| 1k | 346,346 | 307,258 | 420,738 | 441,294 |
| 10k | 27,968 | 27,377 | 37,290 | 40,911 |
| 100k | 2,602 | 3,006 | 3,759 | 4,254 |

### encode, keyword keys

data.json writes keyword keys natively; cheshire likewise. jsonista also
measures a `write-value-as-bytes` variant (`encode-jsonista-kw-bytes`):
8,538,286 / 2,689,386 / 510,551 / 65,743 / 6,072 ops/s from 10b to 100k.

| size | data.json | cheshire | jsonista |
|---|---:|---:|---:|
| 10b | 7,047,371 | 2,156,053 | 8,852,407 |
| 100b | 1,394,450 | 1,489,408 | 3,388,647 |
| 1k | 216,929 | 415,399 | 666,115 |
| 10k | 16,633 | 39,578 | 56,608 |
| 100k | 1,619 | 4,270 | 5,357 |

### decode, keyword keys

data.json with `:key-fn keyword`, cheshire with `(parse-string s true)`.

| size | data.json | cheshire | jsonista |
|---|---:|---:|---:|
| 10b | 7,449,560 | 2,106,946 | 8,814,925 |
| 100b | 1,757,660 | 1,102,503 | 2,520,516 |
| 1k | 325,437 | 268,505 | 443,022 |
| 10k | 27,528 | 24,393 | 39,829 |
| 100k | 2,688 | 2,570 | 3,964 |

## Limitations

1. **Cross-run comparison.** The two jsonista versions cannot share a JVM, so
   the delta table inherits the control-derived noise floor. Within-size
   verdicts above already account for it.
2. **Medium fidelity.** 3×3 s warmup + 5×3 s measurement, not the ~100 s/point
   `:type :quick` config the pre-1.0 README table used. Absolute numbers are
   biased slightly low; error bars were ≤3% for most cells (worst ~12%).
3. **Fixture data** is `dev-resources/json{10b,100b,1k,10k,100k}.json` — object
   shapes typical of API payloads. Workloads dominated by long strings or
   deeply nested arrays may differ.
4. **data.json keyword rows have no cross-run control** (they exist only in
   the Jackson 3 run); the cheshire keyword rows are the control for that
   group.
