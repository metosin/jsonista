package jsonista.jackson;

import clojure.lang.*;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

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
  public Object deserialize(JsonParser p, DeserializationContext ctxt) {
    ValueDeserializer<Object> deser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Object.class));
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
