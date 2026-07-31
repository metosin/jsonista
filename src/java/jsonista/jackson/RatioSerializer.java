package jsonista.jackson;

import clojure.lang.Ratio;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class RatioSerializer extends StdSerializer<Ratio> {

  public RatioSerializer() {
    super(RatioSerializer.class, true);
  }

  @Override
  public void serialize(Ratio value, JsonGenerator gen, SerializationContext provider) {
    gen.writeNumber(value.doubleValue());
  }
}
