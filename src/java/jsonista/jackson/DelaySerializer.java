package jsonista.jackson;

import clojure.lang.Delay;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/** Serializer that mimics the behaviour we had prior to Clojure 1.12.
    The Jackson default serializer now barfs on the Delay objects after Clojure 1.12. */
public class DelaySerializer extends StdSerializer<Delay> {

  public DelaySerializer() {
    super(DateSerializer.class, true);
  }

  @Override
  public void serialize(Delay value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeStartObject();
    gen.writeBooleanField("realized", value.isRealized());
    gen.writeEndObject();
  }
}
