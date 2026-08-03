package jsonista.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateSerializer extends StdSerializer<Date> {
  // DateTimeFormatter is immutable, so serialization needs no synchronization.
  private final DateTimeFormatter formatter;

  public DateSerializer(String dateFormat) {
    super(DateSerializer.class, true);
    formatter = DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneOffset.UTC);
  }

  public DateSerializer() {
    this("yyyy-MM-dd'T'HH:mm:ss'Z'");
  }

  @Override
  public void serialize(Date value, JsonGenerator gen, SerializationContext provider) {
    // Not value.toInstant(): java.sql.Date overrides it to throw.
    gen.writeString(formatter.format(Instant.ofEpochMilli(value.getTime())));
  }
}
