package jsonista.jackson;

import clojure.lang.Keyword;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Key deserializer that turns JSON object keys into Clojure keywords, with a
 * bounded cache in front of {@code Keyword.intern}.
 *
 * <p>{@code Keyword.intern} allocates a fresh {@code Symbol} and hits the
 * global weak-reference keyword table on every call, while JSON key sets are
 * small and repetitive (and Jackson canonicalizes property-name strings per
 * factory), so a String -&gt; Keyword cache turns repeated keys into a single
 * map lookup.
 *
 * <p>The caching strategy - a bounded {@code ConcurrentHashMap} dropped
 * wholesale when it reaches its limit - is the same one jackson-core uses
 * internally for property-name interning, see
 * {@link tools.jackson.core.util.InternCache} (280 entries; on overflow it
 * clears the whole map rather than tracking eviction order). Realistic key
 * sets never reach the bound; unbounded/adversarial key universes degrade to
 * plain {@code Keyword.intern} throughput instead of growing the cache
 * without limit or pinning arbitrarily many keywords against GC.
 */
public class KeywordKeyDeserializer extends KeyDeserializer {

  private static final int MAX_CACHED_KEYS = 1024;

  private final ConcurrentHashMap<String, Keyword> cache = new ConcurrentHashMap<>(64);

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    Keyword kw = cache.get(key);
    if (kw == null) {
      kw = Keyword.intern(key);
      if (cache.size() >= MAX_CACHED_KEYS) {
        cache.clear();
      }
      cache.put(key, kw);
    }
    return kw;
  }
}
