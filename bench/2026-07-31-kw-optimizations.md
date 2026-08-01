# Keyword-keys workload optimizations

**Date:** 2026-07-31
**Branch:** `jackson-3-port`
**Workload:** encode/decode Clojure data with keyword map keys via `keyword-keys-object-mapper` — benchmarked with the `:encode-kw`/`:decode-kw`/`:encode-kw-bytes` groups added in `benchmarks.edn`, cheshire as untouched cross-run control.

All runs: `:fork {:count 1}`, warmup 2×2s, measurement 5×2s, same quiesced machine. Error bars 0.5–4% except where noted. Control (cheshire) drift stayed within ±4% on decode across all runs; encode control was noisier at 10b–100b (up to −25%), so small-payload *encode* deltas across runs are less trustworthy than decode ones.

## Shipped changes

1. **`KeywordKeyDeserializer`: bounded String→Keyword cache** (`c9a801b`) — skips per-key `Keyword.intern` (Symbol allocation + weak-ref table).
2. **`PersistentHashMapDeserializer`: objects ≤8 entries become `PersistentArrayMap`s** (`605c2de`) — matches Clojure map literals; larger objects spill to transient `PersistentHashMap` as before. Also switches to `nextName()`.

## Decode results (`decode-jsonista-kw`, ops/s)

| size | baseline | +intern cache | +array maps | cumulative |
|---|---:|---:|---:|---:|
| 10b | 6,527,526 | 7,336,230 (+12.4%) | 8,972,154 (+22.3%) | **+37.5%** |
| 100b | 1,626,726 | 1,734,666 (+6.6%) | 2,518,588 (+45.2%) | **+54.8%** |
| 1k | 351,678 | 346,686 (−1.4%) | 435,871 (+25.7%) | **+23.9%** |
| 10k | 29,906 | 33,369 (+11.6%) | 36,907 (+10.6%) | **+23.4%** |
| 100k | 2,997 | 3,335 (+11.3%) | 3,684 (+10.5%) | **+22.9%** |

Raw data: `2026-07-31-kw-baseline.txt`, `2026-07-31-kw-opt1-intern-cache.txt`, `2026-07-31-kw-final.txt`.

Behavior note: objects of ≤8 entries now decode to `PersistentArrayMap` (insertion-ordered, like small map literals and like cheshire) instead of `PersistentHashMap`. Equality semantics are unchanged; only code depending on the concrete class or on PHM seq order would notice — both were already unspecified.

## Tried and reverted: SerializedString cache in KeywordSerializer

Hypothesis: caching the pre-quoted form per keyword (`writeName(SerializableString)`) skips the per-write escape scan / UTF-8 encode, the same mechanism that makes POJO property writes fast.

Reality (A/B at `297d17a` vs its parent, back-to-back, `2026-07-31-kw-encode-ab-{HEAD,nocache}.txt`):

| size | write-value-as-**string** Δ | write-value-as-**bytes** Δ |
|---|---:|---:|
| 10b | −2.2% | −1.1% |
| 100b | −3.4% | +11.5% |
| 1k | −4.4% | **+28.7%** |
| 10k | −2.1% | −4.5% |
| 100k | +1.2% | −0.6% |

For short ASCII keys, the ConcurrentHashMap lookup plus the escaping-config guard (needed because `appendQuotedUTF8` bypasses `ESCAPE_NON_ASCII`/`CharacterEscapes` — verified in jackson-core 3.1.5 bytecode) costs more than the escape scan it saves on the char-based generator. Reverted in `bf79b58`.

**If revisiting:** the win is real only on the byte-output path at mid sizes. A cheaper cache (direct-mapped identity slots instead of CHM) might flip the string path from −3% to neutral, but the ceiling there is low. The escaping-behavior tests added with the attempt were kept (`keyword-encoding-test`).

## Attempted later the same day: single-pass untyped deserializer

`ClojureUntypedDeserializer` (one recursive `switch` on token id for `Object`
reads, delegating to any non-stock Map/List deserializers for composition)
**measured performance-neutral** — control-normalized jsonista/cheshire decode
ratios identical within ±3% at every size (`2026-07-31-kw-opt4-untyped.txt`
vs `2026-07-31-kw-final.txt`). Kept anyway: it removes the
`UntypedObjectDeserializer` double dispatch, is where future scalar fast paths
would live, and its composition semantics are pinned by the tagged-module
tests (delegates must be wired in `resolve()`, not `createContextual()` —
runtime `findNonContextualValueDeserializer` lookups skip contextualization).
