package jsonista.jackson;

import clojure.lang.Keyword;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class KeywordSerializer extends StdSerializer<Keyword> {
  private final boolean writeFieldName;

  public KeywordSerializer(boolean writeFieldName) {
    super(KeywordSerializer.class, true);
    this.writeFieldName = writeFieldName;
  }

  @Override
  public void serialize(Keyword value, JsonGenerator gen, SerializationContext provider) {
    String text = value.sym.toString();
    if (writeFieldName) {
      gen.writeName(text);
    } else {
      gen.writeString(text);
    }
  }
}
