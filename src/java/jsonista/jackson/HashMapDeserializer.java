package jsonista.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HashMapDeserializer extends StdDeserializer<Map<Object, Object>> implements ContextualDeserializer {

  private KeyDeserializer _keyDeserializer;
  private JsonDeserializer<?> _valueDeserializer;

  public HashMapDeserializer() {
    super(Map.class);
  }

  public HashMapDeserializer(KeyDeserializer keyDeser, JsonDeserializer<?> valueDeser) {
    this();
    _keyDeserializer = keyDeser;
    _valueDeserializer = valueDeser;
  }

  protected HashMapDeserializer withResolved(KeyDeserializer keyDeser, JsonDeserializer<?> valueDeser) {
    return this._keyDeserializer == keyDeser && this._valueDeserializer == valueDeser ? this : new HashMapDeserializer(keyDeser, valueDeser);
  }

  @Override
  public JsonDeserializer<Map<Object, Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
    JavaType object = ctxt.constructType(Object.class);
    KeyDeserializer keyDeser = ctxt.findKeyDeserializer(object, null);
    JsonDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(keyDeser, valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<Object, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    Map<Object, Object> map = new HashMap<>();
    while (p.nextToken() != JsonToken.END_OBJECT) {
      Object key = _keyDeserializer.deserializeKey(p.getCurrentName(), ctxt);
      p.nextToken();
      Object value = _valueDeserializer.deserialize(p, ctxt);
      map.put(key, value);
    }
    return map;
  }
}
