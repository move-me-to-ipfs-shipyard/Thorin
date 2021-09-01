#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=1 \
    -X:repl ripley.core/process \
    :main-ns find.main
}

main(){
  echo 1
}

uberjar(){
  echo 1
}

release(){
  echo 1
}

"$@"