package jsonista.jackson;

import clojure.lang.ITransientCollection;
import clojure.lang.PersistentVector;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.List;

public class PersistentVectorDeserializer extends StdDeserializer<List<Object>> {

  private ValueDeserializer<Object> _valueDeserializer;

  public PersistentVectorDeserializer() {
    super(List.class);
  }

  protected PersistentVectorDeserializer(ValueDeserializer<Object> valueDeser) {
    this();
    _valueDeserializer = valueDeser;
  }

  @Override
  public ValueDeserializer<List<Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) {
    JavaType object = ctxt.constructType(Object.class);
    ValueDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(valueDeser);
  }

  private ValueDeserializer<List<Object>> withResolved(ValueDeserializer<Object> valueDeser) {
    return this._valueDeserializer == valueDeser ? this : new PersistentVectorDeserializer(valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) {
    ITransientCollection t = PersistentVector.EMPTY.asTransient();
    while (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(_valueDeserializer.deserialize(p, ctxt));
    }
    return (List<Object>) t.persistent();
  }
}
