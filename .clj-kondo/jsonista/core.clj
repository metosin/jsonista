(ns jsonista.core)

(defmacro extend-types
  {:style/indent [1 :defn]}
  [types & specs]
  `(do
     ~@(for [t types]
         `(extend-type ~t ~@specs))))
