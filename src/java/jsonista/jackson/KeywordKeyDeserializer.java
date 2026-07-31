package jsonista.jackson;

import clojure.lang.Keyword;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;

public class KeywordKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) {
    return Keyword.intern(key);
  }
}
