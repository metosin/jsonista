package jsonista.jackson;

import clojure.lang.IFn;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class FunctionalSerializer<T> extends StdSerializer<T> {
  private final IFn encoder;

  public FunctionalSerializer(IFn encoder) {
    super(FunctionalSerializer.class, true);
    this.encoder = encoder;
  }

  @Override
  public void serialize(T value, JsonGenerator gen, SerializationContext provider) {
    encoder.invoke(value, gen);
  }
}
