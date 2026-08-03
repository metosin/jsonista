package jsonista.jackson;

import clojure.lang.ITransientMap;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.Arrays;
import java.util.Map;

public class PersistentHashMapDeserializer extends StdDeserializer<Map<String, Object>> {

  private KeyDeserializer _keyDeserializer;
  private ValueDeserializer<?> _valueDeserializer;

  public PersistentHashMapDeserializer() {
    super(Map.class);
  }

  public PersistentHashMapDeserializer(KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser) {
    this();
    _keyDeserializer = keyDeser;
    _valueDeserializer = valueDeser;
  }

  protected PersistentHashMapDeserializer withResolved(KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser) {
    return this._keyDeserializer == keyDeser && this._valueDeserializer == valueDeser ? this : new PersistentHashMapDeserializer(keyDeser, valueDeser);
  }

  @Override
  public ValueDeserializer<Map<String, Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) {
    JavaType object = ctxt.constructType(Object.class);
    KeyDeserializer keyDeser = ctxt.findKeyDeserializer(object, null);
    ValueDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(keyDeser, valueDeser);
  }

  // Same threshold (in entries) at which Clojure map literals switch from
  // PersistentArrayMap to PersistentHashMap.
  private static final int ARRAY_MAP_THRESHOLD = 8;

  @Override
  public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) {
    return deserializeMap(p, p.nextName(), ctxt, _keyDeserializer, _valueDeserializer);
  }

  /**
   * Builds a Clojure map from an object whose START_OBJECT has been consumed;
   * {@code name} is the first property name ({@code null} for an empty
   * object). Shared with {@link ClojureUntypedDeserializer}'s inlined path.
   *
   * Small JSON objects (the common case) become PersistentArrayMaps, like
   * small Clojure map literals - cheaper to build (no hashing, no HAMT
   * nodes) and faster to look up. Larger objects spill into a transient
   * PersistentHashMap. nextName() is jackson-core's fused
   * nextToken()+currentName() fast path; it returns null on END_OBJECT.
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> deserializeMap(JsonParser p, String name, DeserializationContext ctxt,
                                            KeyDeserializer keyDeserializer, ValueDeserializer<?> valueDeserializer) {
    final Object[] buf = new Object[2 * ARRAY_MAP_THRESHOLD];
    int n = 0;
    for (; name != null && n < buf.length; name = p.nextName()) {
      buf[n] = keyDeserializer.deserializeKey(name, ctxt);
      p.nextToken();
      buf[n + 1] = valueDeserializer.deserialize(p, ctxt);
      n += 2;
    }
    if (name == null) {
      // createAsIfByAssoc gives assoc semantics (last value wins) for
      // duplicate keys, matching the previous transient-assoc behavior.
      return (Map<String, Object>) PersistentArrayMap.createAsIfByAssoc(n == buf.length ? buf : Arrays.copyOf(buf, n));
    }
    ITransientMap t = PersistentHashMap.EMPTY.asTransient();
    for (int i = 0; i < n; i += 2) {
      t = t.assoc(buf[i], buf[i + 1]);
    }
    do {
      Object key = keyDeserializer.deserializeKey(name, ctxt);
      p.nextToken();
      t = t.assoc(key, valueDeserializer.deserialize(p, ctxt));
    } while ((name = p.nextName()) != null);

    // t.persistent() returns a PersistentHashMap, which is a Map.
    return (Map<String, Object>) t.persistent();
  }
}
