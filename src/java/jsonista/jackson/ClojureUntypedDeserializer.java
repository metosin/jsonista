package jsonista.jackson;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonTokenId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.List;
import java.util.Map;

/**
 * Untyped (Object-targeted) deserializer that builds Clojure data structures
 * directly. Replaces databind's UntypedObjectDeserializer, which in the
 * presence of custom Map/List deserializers routes every JSON value through
 * an extra dispatch (untyped switch -> delegate deserializer). Here the
 * whole tree walk is one recursive switch.
 *
 * Composition is preserved: when something other than the stock jsonista
 * deserializer is registered for Map or List (java-collection-module,
 * jsonista.tagged, user modules), objects/arrays are delegated to it exactly
 * like databind's untyped deserializer would.
 */
public class ClojureUntypedDeserializer extends StdDeserializer<Object> {

  private KeyDeserializer _keyDeserializer;
  private ValueDeserializer<Object> _mapDelegate;   // non-null: a non-stock Map deserializer is registered
  private ValueDeserializer<Object> _listDelegate;  // non-null: a non-stock List deserializer is registered

  public ClojureUntypedDeserializer() {
    super(Object.class);
  }

  @Override
  public void resolve(DeserializationContext ctxt) {
    // resolve() - not createContextual() - so the wiring lands on the single
    // cached instance. Runtime lookups via findNonContextualValueDeserializer
    // (e.g. from TaggedValueOrPersistentVectorDeserializer) return the cached
    // instance without contextualization, exactly like databind's own
    // UntypedObjectDeserializer, which uses this same hook.
    _keyDeserializer = ctxt.findKeyDeserializer(ctxt.constructType(Object.class), null);
    ValueDeserializer<Object> mapDeser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(Map.class));
    ValueDeserializer<Object> listDeser = ctxt.findNonContextualValueDeserializer(ctxt.constructType(List.class));
    // Exact-class checks: subclasses or replacements must go through the
    // delegate path (TaggedValueOrPersistentVectorDeserializer etc.).
    _mapDelegate = mapDeser != null && mapDeser.getClass() == PersistentHashMapDeserializer.class ? null : mapDeser;
    _listDelegate = listDeser != null && listDeser.getClass() == PersistentVectorDeserializer.class ? null : listDeser;
  }

  @Override
  public Object deserialize(JsonParser p, DeserializationContext ctxt) {
    switch (p.currentTokenId()) {
      case JsonTokenId.ID_START_OBJECT:
        if (_mapDelegate != null) {
          return _mapDelegate.deserialize(p, ctxt);
        }
        return PersistentHashMapDeserializer.deserializeMap(p, p.nextName(), ctxt, _keyDeserializer, this);
      case JsonTokenId.ID_PROPERTY_NAME:
        // mid-object entry (e.g. via readerForUpdating), like databind's
        // untyped deserializer supports
        if (_mapDelegate != null) {
          return _mapDelegate.deserialize(p, ctxt);
        }
        return PersistentHashMapDeserializer.deserializeMap(p, p.currentName(), ctxt, _keyDeserializer, this);
      case JsonTokenId.ID_START_ARRAY:
        if (_listDelegate != null) {
          return _listDelegate.deserialize(p, ctxt);
        }
        return PersistentVectorDeserializer.deserializeVector(p, ctxt, this);
      case JsonTokenId.ID_STRING:
        return p.getString();
      case JsonTokenId.ID_NUMBER_INT:
        // Integer/Long/BigInteger by magnitude, honoring USE_BIG_INTEGER_FOR_INTS
        // and USE_LONG_FOR_INTS - same behavior as databind's untyped deserializer
        if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
          return _coerceIntegral(p, ctxt);
        }
        return p.getNumberValue();
      case JsonTokenId.ID_NUMBER_FLOAT:
        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
          return p.getDecimalValue();
        }
        return p.getNumberValue();
      case JsonTokenId.ID_TRUE:
        return Boolean.TRUE;
      case JsonTokenId.ID_FALSE:
        return Boolean.FALSE;
      case JsonTokenId.ID_NULL:
        return null;
      case JsonTokenId.ID_EMBEDDED_OBJECT:
        // non-JSON formats (e.g. CBOR binary values)
        return p.getEmbeddedObject();
      default:
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }
  }
}
