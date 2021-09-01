#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=1 \
    -X:repl ripley.core/process \
    :main-ns find.main
}

main(){
  clojure \
    -J-Dclojure.core.async.pool-size=1 \
    -M -m find.main
}

uberjar(){
  echo 1
}

release(){
  echo 1
}

"$@"