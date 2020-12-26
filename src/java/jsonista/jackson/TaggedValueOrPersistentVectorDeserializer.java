package jsonista.jackson;

import clojure.lang.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TaggedValueOrPersistentVectorDeserializer extends StdDeserializer<Object> {

  private final Map<String, IFn> decoders;

  public TaggedValueOrPersistentVectorDeserializer(Map<String, IFn> decoders) {
    super(List.class);
    this.decoders = decoders;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonDeserializer<Object> deser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Object.class));
    ITransientCollection t = PersistentVector.EMPTY.asTransient();
    if (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(deser.deserialize(p, ctxt));
      Object maybeTag = ((Indexed) t).nth(0);
      if (maybeTag instanceof String) {
        IFn decode = decoders.get(maybeTag);
        if (decode != null) {
          /* Jump to keyword. */
          p.nextValue();
          Object o = decode.invoke(deser.deserialize(p, ctxt));
          /* Jump to end of list. */
          p.nextValue();
          return o;
        }
      }
    } else {
      return t.persistent();
    }

    while (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(deser.deserialize(p, ctxt));
    }
    return t.persistent();
  }
}
