package jsonista.jackson;

import clojure.lang.ITransientMap;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class PersistentHashMapDeserializer extends StdDeserializer<Map<String, Object>> implements ContextualDeserializer {

  private KeyDeserializer _keyDeserializer;
  private JsonDeserializer<?> _valueDeserializer;

  public PersistentHashMapDeserializer() {
    super(Map.class);
  }

  public PersistentHashMapDeserializer(KeyDeserializer keyDeser, JsonDeserializer<?> valueDeser) {
    this();
    _keyDeserializer = keyDeser;
    _valueDeserializer = valueDeser;
  }

  protected PersistentHashMapDeserializer withResolved(KeyDeserializer keyDeser, JsonDeserializer<?> valueDeser) {
    return this._keyDeserializer == keyDeser && this._valueDeserializer == valueDeser ? this : new PersistentHashMapDeserializer(keyDeser, valueDeser);
  }

  @Override
  public JsonDeserializer<Map<String, Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
    JavaType object = ctxt.constructType(Object.class);
    KeyDeserializer keyDeser = ctxt.findKeyDeserializer(object, null);
    JsonDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(keyDeser, valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    // Phase 1: collect up to 8 entries for a PersistentArrayMap
    Object[] entries = new Object[16];
    int size = 0;
    while (size < 8) {
      if (p.nextToken() == JsonToken.END_OBJECT) {
        return (Map<String, Object>) PersistentArrayMap.createAsIfByAssoc(Arrays.copyOf(entries, size * 2));
      }
      Object key = _keyDeserializer.deserializeKey(p.getCurrentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      entries[size * 2] = key;
      entries[size * 2 + 1] = value;
      size++;
    }

    // Phase 2: overflow into a PersistentHashMap
    ITransientMap t = PersistentHashMap.EMPTY.asTransient();
    for (int i = 0; i < 16; i += 2) {
      t = t.assoc(entries[i], entries[i + 1]);
    }
    while (p.nextToken() != JsonToken.END_OBJECT) {
      Object key = _keyDeserializer.deserializeKey(p.getCurrentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      t = t.assoc(key, value);
    }
    return (Map<String, Object>) t.persistent();
  }
}
