package jsonista.jackson;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class FunctionalKeywordSerializer extends StdSerializer<Keyword> {
  private final IFn encoder;

  public FunctionalKeywordSerializer(IFn encoder) {
    super(FunctionalKeywordSerializer.class, true);
    this.encoder = encoder;
  }

  @Override
  public void serialize(Keyword value, JsonGenerator gen, SerializationContext provider) {
    gen.writeName(String.valueOf(encoder.invoke(value)));
  }
}
