package jsonista.jackson;

import clojure.lang.Delay;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/** Serializer that mimics the behaviour we had prior to Clojure 1.12.
    The Jackson default serializer now barfs on the Delay objects after Clojure 1.12. */
public class DelaySerializer extends StdSerializer<Delay> {

  public DelaySerializer() {
    super(DateSerializer.class, true);
  }

  @Override
  public void serialize(Delay value, JsonGenerator gen, SerializationContext provider) {
    gen.writeStartObject();
    gen.writeBooleanProperty("realized", value.isRealized());
    gen.writeEndObject();
  }
}
