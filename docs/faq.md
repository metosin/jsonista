# Frequently Asked Questions

## Why am I getting a NoClassDefFoundError?

```
java.lang.NoClassDefFoundError: tools/jackson/core/exc/InputCoercionException,
compiling:(jsonista/core.clj:79:38)
```

If you're getting this kind of error message when requiring `jsonista.core` or otherwise using jsonista (e.g. via [muuntaja](https://github.com/metosin/muuntaja)),
the problem is that **you're depending on different versions of `jackson-core` and `jackson-databind`**. You need to depend on the same versions of both for jsonista to work correctly.

Run `lein deps :tree` (Leiningen users) or `clj -Stree` (deps.edn users) in your project and look for lines with `tools.jackson.core/jackson-core` and `tools.jackson.core/jackson-databind`. If their versions do not match, that's the problem.

Note that Jackson 2 and Jackson 3 can coexist on the same classpath without conflict — they live under different Maven groupIds (`com.fasterxml.jackson.*` vs. `tools.jackson.*`) and different Java packages, so having both present is not, by itself, the cause of this error. (This project's own `:dev` profile does exactly that: cheshire pulls in Jackson 2 while jsonista uses Jackson 3.) Since jsonista moved to Jackson 3, this error now almost always means **two conflicting Jackson 3 versions** rather than a 2-vs-3 clash.

Possible solutions:

* Add the same versions of `jackson-core` and `jackson-databind` as dependencies for your project. For example, if you use Leiningen, add these lines to the `:dependencies` vector in your `project.clj`:

```clojure
[tools.jackson.core/jackson-core "3.2.1"]
[tools.jackson.core/jackson-databind "3.2.1"]
```

* Look at the dependency tree and see which library is bringing in the wrong version of `jackson-core` and use `:exclusions` to prevent it from happening.

## Writing raw values

Already encoded JSON values can be used with RawValue marker class:

```
(import '[tools.jackson.databind.util RawValue])
(json/write-value-as-string
  {:version 555
   :foobar (RawValue. "{\"foo\": \"bar\"}")})
```
