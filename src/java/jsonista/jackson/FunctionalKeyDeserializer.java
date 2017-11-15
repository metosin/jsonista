package jsonista.jackson;

import clojure.lang.IFn;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class FunctionalKeyDeserializer extends KeyDeserializer {
  private final IFn encoder;

  public FunctionalKeyDeserializer(IFn encoderFunction) {
    super();
    encoder = encoderFunction;
  }

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
    return encoder.invoke(key);
  }
}
