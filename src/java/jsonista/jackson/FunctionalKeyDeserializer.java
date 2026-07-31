package jsonista.jackson;

import clojure.lang.IFn;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

public class FunctionalKeyDeserializer extends KeyDeserializer {
  private final IFn encoder;

  public FunctionalKeyDeserializer(IFn encoder) {
    this.encoder = encoder;
  }

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    return encoder.invoke(key);
  }
}
