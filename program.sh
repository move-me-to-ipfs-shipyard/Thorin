#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=1 \
    -X:repl Ripley.core/process \
    :main-ns Find.main
}

main(){
  clojure \
    -J-Dclojure.core.async.pool-size=1 \
    -M -m Find.main
}

uberjar(){

  clojure \
    -X:identicon Zazu.core/process \
    :word '"Find"' \
    :filename '"out/identicon/icon.png"' \
    :size 256

  clojure \
    -X:uberjar Genie.core/process \
    :main-ns Find.main \
    :filename '"out/Find.jar"' \
    :paths '["src" "out/identicon"]'
}

release(){
  uberjar
}

"$@"