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
    ITransientMap t = PersistentHashMap.EMPTY.asTransient();
    while (p.nextToken() != JsonToken.END_OBJECT) {
      Object key = _keyDeserializer.deserializeKey(p.getCurrentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      t = t.assoc(key, value);
    }

    // t.persistent() returns a PersistentHashMap, which is a Map.
    return (Map<String, Object>) t.persistent();
  }
}
