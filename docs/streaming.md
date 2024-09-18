# Streaming JSON with Jsonista

## JSON Lines (aka JSONL)

Sometimes you want to store a stream of JSON objects in a file. This is common for things like logging.
This pattern is often called [JSON Lines](https://jsonlines.org/).

### Writing

```clj
(jsonista.core/write-values (io/output-stream "/tmp/foo.json") [{"foo" 1} {"bar" 1}])
```

For actual streaming, use a lazy sequence or an eduction instead of a
vector. For example:

```clj
(jsonista.core/write-values
 (io/output-stream "/tmp/foo.json")
 (eduction (map (fn [i] {:i i})) (range 100)))
```

Alternatively, you can use Jackson's imperative API directly:

```clj
(let [obj-mapper (jsonista.core/object-mapper {:close false})]
  (with-open [out (io/output-stream "/tmp/foo.json")
              wrt (io/writer out)]
    (jsonista.core/write-value wrt {"foo" 1} obj-mapper)
    (.write wrt "\n")
    (jsonista.core/write-value wrt {"bar" 1} obj-mapper)))
```

### Reading

```clj
(into [] (jsonista.core/read-values (io/input-stream "/tmp/foo.json")))
```

## Top-level array

Instead of being separated on separate lines, sometimes you just want
a big JSON array, but don't want to keep all of the data in memory at
once.

### Writing

Use `jsonista.core/write-values-as-array`, which works just like `jsonista.core/write-values`.

### Reading

Use `jsonista.core/read-values`, it autodetects the format.

## An array inside an object

Sometimes you need to stream an array that sits inside an object. For this, it's best to drop down to the Jackson [JsonParser API](https://javadoc.io/static/com.fasterxml.jackson.core/jackson-core/2.18.0-rc1/com/fasterxml/jackson/core/JsonParser.html)

```clj
(let [input "{\"foo\": 1, \"bars\": [{\"bar\": 2},{\"bar\": 3}], \"close\": \"end\"}"
      obj-mapper (jsonista.core/object-mapper)]
   (with-open [rdr (java.io.StringReader. input)]
      (let [p (.. obj-mapper getFactory (createParser rdr))]
        ;; position cursor to start of first entry in "bars"
        (.nextToken p) ; START_OBJECT
        (.nextToken p) ; FIELD_NAME "foo"
        (.nextToken p) ; VALUE_NUMBER_INT 1
        (.nextToken p) ; FIELD_NAME "bar"
        (.nextToken p) ; START_ARRAY
        (.nextToken p) ; START_OBJECT
        ;; grab all entries, ignore rest of input
        (doall (iterator-seq (.readValuesAs p Object))))))
```
