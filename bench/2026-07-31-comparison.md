# jsonista: Jackson 2 vs Jackson 3 — benchmark comparison

**Date:** 2026-07-31
**Branch:** `jackson-3-port`
**Before:** jsonista 1.0.0 on Jackson 2.21.2 — `bench/2026-07-31-jackson2-baseline.txt` (283s)
**After:** jsonista 2.0.0-SNAPSHOT on Jackson 3.1.5 — `bench/2026-07-31-jackson3-ported.txt` (282s)

## Headline

**At payload sizes of 100 bytes and above, the port produced no measurable performance change.** Every jsonista encode and decode result from `100b` through `100k` falls inside the noise floor established by the controls.

**At 10 bytes there is a signal suggesting Jackson 3 is slower, but this benchmark cannot quantify it.** See "The 10b anomaly" below — it is the one result that warrants follow-up, and it should not be reported as a number.

## Method

Both runs used an identical configuration on the same quiesced machine, same JDK, back to back:

```
lein jmh '{:file "benchmarks.edn",
           :fork {:count 1 :warmups 0},
           :warmup {:iterations 1 :time [1 :s]},
           :measurement {:iterations 5 :time [1 :s]},
           :format [:table :pprint], :output ...}'
```

- **JDK:** OpenJDK 25.0.2 (Corretto), Apple Silicon macOS
- **Sweep:** unchanged `benchmarks.edn` — 4 implementations × 2 operations × 5 payload sizes = 40 measurements per run
- **Controls:** cheshire and data.json are untouched by the port. Cheshire deliberately remains on Jackson 2. Their cross-run drift is therefore pure measurement noise, and it defines the threshold below which no jsonista result can be believed.

### Noise floor

| statistic | value |
|---|---|
| mean absolute control drift | **4.4%** |
| max absolute control drift | **20.6%** (`decode-data-json` @ 10b) |

The max is the honest threshold. It is dominated by the smallest payload: the two largest control drifts are both at `10b` (+20.6% and +12.7%), while every control at `1k` and above stayed within 6.2%.

## jsonista results

Ranked against the 20.6% noise floor.

| benchmark | size | Jackson 2 (ops/s) | Jackson 3 (ops/s) | change | verdict |
|---|---|---:|---:|---:|---|
| encode | 10b | 16,543,515 ±25.8% | 9,223,559 ±2.0% | −44.2% | **see anomaly below** |
| encode | 100b | 3,905,376 | 3,469,696 | −11.2% | no measurable change |
| encode | 1k | 652,802 | 642,577 | −1.6% | no measurable change |
| encode | 10k | 57,594 | 56,105 | −2.6% | no measurable change |
| encode | 100k | 5,498 | 5,483 | −0.3% | no measurable change |
| decode | 10b | 8,298,341 | 7,501,714 | −9.6% | no measurable change |
| decode | 100b | 1,709,760 | 1,652,347 | −3.4% | no measurable change |
| decode | 1k | 345,037 | 336,976 | −2.3% | no measurable change |
| decode | 10k | 30,041 ±15.4% | 33,441 | +11.3% | no measurable change |
| decode | 100k | 3,232 | 3,253 | +0.7% | no measurable change |

Nine of ten results are inside the noise floor. Note that several of them lean slightly negative (−1.6%, −2.3%, −2.6%, −3.4%); that is suggestive of a small real cost but is not separable from drift at this fidelity, and must not be reported as one.

## The 10b anomaly

`encode-jsonista` at 10b fell 44.2%, which exceeds even the 20.6% floor. Three observations bear on whether it is real:

1. **The raw-Jackson ceiling moved the same way.** Raw Jackson encode at 10b fell 29.5% and decode 24.0% between the two engine versions. Whatever is happening is in Jackson, not in jsonista's wrapper.
2. **The Jackson 2 measurement is untrustworthy.** The baseline figure carries ±25.8% error (±4.27M on 16.5M); the Jackson 3 figure carries ±2.0%. A 44% delta measured against a number that uncertain cannot be quantified.
3. **10b is where this configuration is weakest.** A 10-byte encode takes tens of nanoseconds, so throughput at that size is dominated by JIT state — and this run allots one second of warmup. Both controls posted their worst drift at exactly this size.

**Conclusion:** directionally consistent with a genuine Jackson 3 regression on very small payloads, but unquantified. Do not publish a percentage.

**To resolve it**, run only the 10b parameter at full fidelity (`:type :quick`, i.e. 5×10s warmup + 5×10s measurement). Eight benchmark points at roughly 105s each is about 14 minutes — far cheaper than the ~67 minutes a full-sweep high-fidelity run costs.

## Raw Jackson 2 engine vs Jackson 3 engine

Reference only. Unlike the jsonista rows, these compare two *different engines*, not the same code before and after a port.

| benchmark | size | Jackson 2 | Jackson 3 | change |
|---|---|---:|---:|---:|
| encode | 10b | 12,884,456 | 9,082,575 | −29.5% |
| encode | 100b | 3,777,370 | 3,810,813 | +0.9% |
| encode | 1k | 701,524 | 706,696 | +0.7% |
| encode | 10k | 57,667 | 57,270 | −0.7% |
| encode | 100k | 5,475 | 5,524 | +0.9% |
| decode | 10b | 11,294,452 | 8,579,239 | −24.0% |
| decode | 100b | 2,663,499 | 2,424,269 | −9.0% |
| decode | 1k | 448,611 | 436,271 | −2.8% |
| decode | 10k | 39,594 | 40,539 | +2.4% |
| decode | 100k | 3,965 | 4,170 | +5.2% |

From `1k` upward the two engines are indistinguishable. The divergence is confined to the two smallest payloads.

## Limitations — read before quoting any of this

1. **These numbers are not comparable to the README's published table.** The README used `:type :quick` (5×10s warmup + 5×10s measurement), which measured ~67 minutes per run. This configuration uses 1×1s warmup + 5×1s measurement to fit a ~5 minute budget. One second of warmup leaves the JIT only partly settled, so absolute throughput is biased low and the shape across payload sizes is distorted. **Do not paste this table into the README as a replacement.**

2. **The comparison is valid despite that**, because both runs share the identical configuration, so systematic warmup bias largely cancels in the before/after ratio. Random variance does not cancel, which is precisely what the control-derived noise floor accounts for.

3. **A 20.6% floor is coarse.** Any real regression smaller than that is invisible here. The consistent small negatives across the mid-range results hint that something may be there; confirming it needs a high-fidelity run.

4. **`FAIL_ON_TRAILING_TOKENS` costs nothing measurable** — measured directly, see below.

5. **What this benchmark measures is not stock jsonista.** `encode-jsonista`/`decode-jsonista` are defined at `test/jsonista/jmh.clj:46-52` against a mapper carrying a `jsonista.tagged` module for `Keyword`, shadowing the plainer definitions at lines 34-36. This is pre-existing and identical across both runs, so the comparison holds — but the absolute figures are not those of `default-object-mapper`.

6. **The one behavior change in the port does not affect these numbers.** `jsonista.tagged/encode-collection` had to change because Jackson 3 removed `getCodec()`, and it now uses `writePOJO` instead of serializing each element to a `String`. No benchmark in `benchmarks.edn` exercises that path — the tagged mapper here registers only `encode-keyword` — so no result above is contaminated by it.

## Measured: the cost of `FAIL_ON_TRAILING_TOKENS`

Jackson 3 enables `FAIL_ON_TRAILING_TOKENS` by default, so every decode now verifies there is no extra content after the parsed value. The migration guide warns this "adds a small amount of overhead". Measured directly, **it does not**.

Raw data: `bench/2026-07-31-trailing-tokens.txt`. Harness: `test/jsonista/trailing_bench.clj` + `benchmarks-trailing.edn`.

This experiment is **more sensitive than the main comparison**, deliberately:

- Both mappers are the same object rebuilt with only that one feature toggled, so nothing else can differ. Verified before measuring: the strict mapper throws `StreamReadException` on trailing content, the lenient one returns the value, and both return identical results for clean input.
- Both variants run in a **single JMH invocation**, making this a within-run comparison with no cross-run drift — the entire reason the main comparison's noise floor is 20.6%.
- Only 10 measurement points instead of 40, which buys **5×2s warmup instead of 1×1s** at the same wall-clock cost. Error bars came in at 0.2-4.7%.

| size | strict (default) | lenient (feature off) | delta | verdict |
|---|---:|---:|---:|---|
| 10b | 6,869,655 ±0.21% | 6,703,823 ±1.01% | +2.5% | strict *faster* |
| 100b | 1,753,531 ±3.46% | 1,763,596 ±4.72% | −0.6% | below resolution |
| 1k | 335,568 ±1.45% | 345,293 ±1.36% | −2.8% | strict slower |
| 10k | 32,310 ±2.83% | 31,883 ±2.63% | +1.3% | below resolution |
| 100k | 3,265 ±2.76% | 3,245 ±4.00% | +0.6% | below resolution |

*delta = (strict − lenient) / lenient. Negative means the strict default costs throughput.*

**Conclusion: no measurable cost.** The deltas scatter around zero with a mean of +0.2% and no consistent sign. The two points that exceed their own error bars disagree with each other — 10b says the strict default is *faster*, 1k says slower — which is what noise looks like, not a systematic effect. Even at 100k, where a trailing-token scan would be cheapest relative to parse work, the difference is +0.6%.

Two consequences:

1. **Do not disable `FAIL_ON_TRAILING_TOKENS` for performance.** There is nothing to reclaim. Keeping Jackson 3's stricter default costs nothing, so the only reason to disable it would be compatibility with callers that rely on lenient parsing.
2. **This does not explain the 10b anomaly.** That anomaly is in *encode*, and this feature affects only decode — so it was never a candidate. It remains unexplained and still warrants the targeted high-fidelity run described above.

## Bottom line

The port is performance-neutral for realistic payloads (≥100 bytes) within this benchmark's ability to detect. The very-small-payload behavior needs one targeted high-fidelity run before any claim is made about it in the release notes.
