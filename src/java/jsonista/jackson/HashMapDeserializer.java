package jsonista.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.HashMap;
import java.util.Map;

public class HashMapDeserializer extends StdDeserializer<Map<Object, Object>> {

  private KeyDeserializer _keyDeserializer;
  private ValueDeserializer<?> _valueDeserializer;

  public HashMapDeserializer() {
    super(Map.class);
  }

  public HashMapDeserializer(KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser) {
    this();
    _keyDeserializer = keyDeser;
    _valueDeserializer = valueDeser;
  }

  protected HashMapDeserializer withResolved(KeyDeserializer keyDeser, ValueDeserializer<?> valueDeser) {
    return this._keyDeserializer == keyDeser && this._valueDeserializer == valueDeser ? this : new HashMapDeserializer(keyDeser, valueDeser);
  }

  @Override
  public ValueDeserializer<Map<Object, Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) {
    JavaType object = ctxt.constructType(Object.class);
    KeyDeserializer keyDeser = ctxt.findKeyDeserializer(object, null);
    ValueDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(keyDeser, valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Object> deserialize(JsonParser p, DeserializationContext ctxt) {
    Map<Object, Object> map = new HashMap<>();
    while (p.nextToken() != JsonToken.END_OBJECT) {
      Object key = _keyDeserializer.deserializeKey(p.currentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      map.put(key, value);
    }
    return map;
  }
}
