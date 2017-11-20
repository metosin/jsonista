package jsonista.jackson;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class FunctionalKeywordSerializer extends StdSerializer<Keyword> {
  private final IFn encoder;

  public FunctionalKeywordSerializer(IFn encoder) {
    super(FunctionalKeywordSerializer.class, true);
    this.encoder = encoder;
  }

  @Override
  public void serialize(Keyword value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeFieldName(String.valueOf(encoder.invoke(value)));
  }
}
