package jsonista.jackson;

import clojure.lang.Symbol;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SymbolSerializer extends StdSerializer<Symbol> {

  public SymbolSerializer() {
    super(SymbolSerializer.class, true);
  }

  @Override
  public void serialize(Symbol value, JsonGenerator gen, SerializationContext provider) {
    gen.writeString(value.toString());
  }
}
