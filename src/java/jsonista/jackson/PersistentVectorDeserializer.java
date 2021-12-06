package jsonista.jackson;

import clojure.lang.ITransientCollection;
import clojure.lang.PersistentVector;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PersistentVectorDeserializer extends StdDeserializer<List<Object>> implements ContextualDeserializer {

  private JsonDeserializer<Object> _valueDeserializer;

  public PersistentVectorDeserializer() {
    super(List.class);
  }

  protected PersistentVectorDeserializer(JsonDeserializer<Object> valueDeser) {
    this();
    _valueDeserializer = valueDeser;
  }

  @Override
  public JsonDeserializer<List<Object>> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) throws JsonMappingException {
    JavaType object = ctxt.constructType(Object.class);
    JsonDeserializer<Object> valueDeser = ctxt.findNonContextualValueDeserializer(object);
    return this.withResolved(valueDeser);
  }

  private JsonDeserializer<List<Object>> withResolved(JsonDeserializer<Object> valueDeser) {
    return this._valueDeserializer == valueDeser ? this : new PersistentVectorDeserializer(valueDeser);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    ITransientCollection t = PersistentVector.EMPTY.asTransient();
    while (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(_valueDeserializer.deserialize(p, ctxt));
    }
    return (List<Object>) t.persistent();
  }
}
