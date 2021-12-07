package jsonista.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArrayListDeserializer extends StdDeserializer<List<Object>> implements ContextualDeserializer {

  private JsonDeserializer<Object> _valueDeserializer;

  public ArrayListDeserializer() {
    super(List.class);
  }

  public ArrayListDeserializer(JsonDeserializer<Object> valueDeser) {
    this();
    _valueDeserializer = valueDeser;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
    JavaType object = ctxt.constructType(Object.class);
    JsonDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(valueDeser);
  }

  private JsonDeserializer<List<Object>> withResolved(JsonDeserializer<Object> valueDeser) {
    return this._valueDeserializer == valueDeser ? this : new ArrayListDeserializer(valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ArrayList<Object> list = new ArrayList<>();

    JsonDeserializer<Object> deser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Object.class));
    while (p.nextValue() != JsonToken.END_ARRAY) {
      list.add(deser.deserialize(p, ctxt));
    }
    return list;
  }
}
