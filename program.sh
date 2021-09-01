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

  clojure \
    -X:identicon zazu.core/process \
    :word '"find"' \
    :filename '"out/identicon/icon.png"' \
    :size 256

  clojure \
    -X:uberjar genie.core/process \
    :main-ns find.main \
    :filename '"out/find.jar"' \
    :paths '["src" "out/identicon"]'
}

release(){
  uberjar
}

"$@"