# Frequently Asked Questions

## Why am I getting a NoClassDefFoundError?

```
java.lang.NoClassDefFoundError: com/fasterxml/jackson/core/exc/InputCoercionException,
compiling:(jsonista/core.clj:79:38)
```

If you're getting this kind of error message when requiring `jsonista.core` or otherwise using jsonista (e.g. via [muuntaja](https://github.com/metosin/muuntaja)),
the problem is that **you're depending on different versions of `jackson-core` and `jackson-databind`**. You need to depend on the same versions of both for jsonista to work correctly.

Run `lein deps :tree` (Leiningen users) or `clj -Stree` (deps.edn users) in your project and look for lines with `com.fasterxml.jackson.core/jackson-core` and `com.fasterxml.jackson.core/jackson-core`. If their versions do not match, that's the problem.

Possible solutions:

* Add the same versions of `jackson-core` and `jackson-databind` as dependencies for your project. For example, if you use Leiningen, add these lines to the `:dependencies` vector in your `project.clj`:

```clojure
[com.fasterxml.jackson.core/jackson-core "2.10.2"]
[com.fasterxml.jackson.core/jackson-databind "2.10.2"]
```

* Look at the dependency tree and see which library is bringing in the wrong version of `jackson-core` and use `:exclusions` to prevent it from happening.
