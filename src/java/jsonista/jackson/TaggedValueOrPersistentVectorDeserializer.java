package jsonista.jackson;

import clojure.lang.Keyword;
import clojure.lang.ITransientCollection;
import clojure.lang.PersistentVector;
import clojure.lang.Indexed;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

public class TaggedValueOrPersistentVectorDeserializer extends StdDeserializer<Object> {
  private final String tag;

  public TaggedValueOrPersistentVectorDeserializer(String tag) {
    super(List.class);
    this.tag = tag;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonDeserializer<Object> deser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Object.class));
    ITransientCollection t = PersistentVector.EMPTY.asTransient();
    if (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(deser.deserialize(p, ctxt));
      Object maybeTag = ((Indexed) t).nth(0);
      if (maybeTag instanceof String && tag.equals(maybeTag)) {
        /* Jump to keyword. */
        p.nextValue();
        Keyword keyword = Keyword.intern((String) deser.deserialize(p, ctxt));
        /* Jump to end of list. */
        p.nextValue();
        return keyword;
      }
    } else {
      return (List<Object>) t.persistent();
    }

    while (p.nextValue() != JsonToken.END_ARRAY) {
      t = t.conj(deser.deserialize(p, ctxt));
    }
    return (List<Object>) t.persistent();
  }
}
