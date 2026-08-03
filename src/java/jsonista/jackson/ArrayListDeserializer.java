package jsonista.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.List;

public class ArrayListDeserializer extends StdDeserializer<List<Object>> {

  private ValueDeserializer<Object> _valueDeserializer;

  public ArrayListDeserializer() {
    super(List.class);
  }

  public ArrayListDeserializer(ValueDeserializer<Object> valueDeser) {
    this();
    _valueDeserializer = valueDeser;
  }

  @Override
  public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) {
    JavaType object = ctxt.constructType(Object.class);
    ValueDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(valueDeser);
  }

  private ValueDeserializer<List<Object>> withResolved(ValueDeserializer<Object> valueDeser) {
    return this._valueDeserializer == valueDeser ? this : new ArrayListDeserializer(valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) {
    ArrayList<Object> list = new ArrayList<>();

    ValueDeserializer<Object> deser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Object.class));
    while (p.nextValue() != JsonToken.END_ARRAY) {
      list.add(deser.deserialize(p, ctxt));
    }
    return list;
  }
}
