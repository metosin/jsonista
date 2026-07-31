package jsonista.jackson;

import clojure.lang.ITransientMap;
import clojure.lang.PersistentHashMap;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

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

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) {
    ITransientMap t = PersistentHashMap.EMPTY.asTransient();
    while (p.nextToken() != JsonToken.END_OBJECT) {
      Object key = _keyDeserializer.deserializeKey(p.currentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      t = t.assoc(key, value);
    }

    // t.persistent() returns a PersistentHashMap, which is a Map.
    return (Map<String, Object>) t.persistent();
  }
}
