package jsonista.jackson;

import clojure.lang.IFn;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class FunctionalSerializer<T> extends StdSerializer<T> {
  private final IFn encoder;

  public FunctionalSerializer(IFn encoder) {
    super(FunctionalSerializer.class, true);
    this.encoder = encoder;
  }

  @Override
  public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    encoder.invoke(value, gen);
  }
}
