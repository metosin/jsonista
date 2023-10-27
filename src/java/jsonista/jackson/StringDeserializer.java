package jsonista.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class StringDeserializer extends StdDeserializer<String> {
    /*
     * StringDeserializer focuses on _deserializing_ String values for those that have `null` specified to `clojure.core/nil`
     */
    public static final String NULL = "null";

    public StringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        String textValue = p.getText();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        else if (t == JsonToken.VALUE_STRING && textValue.equals(NULL)) {
            return null;
        }
        return textValue;
    };
}
