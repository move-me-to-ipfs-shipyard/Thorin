#!/bin/bash

repl(){
  clj \
    -J-Dclojure.core.async.pool-size=1 \
    -X:repl Ripley.core/process \
    :main-ns Bilbo.main
}

main(){
  clojure \
    -J-Dclojure.core.async.pool-size=1 \
    -M -m Bilbo.main
}

uberjar(){

  clojure \
    -X:identicon Zazu.core/process \
    :word '"Bilbo"' \
    :filename '"out/identicon/icon.png"' \
    :size 256

  rm -rf out/*.jar
  clojure \
    -X:uberjar Genie.core/process \
    :main-ns Bilbo.main \
    :filename "\"out/Bilbo-$(git rev-parse --short HEAD).jar\"" \
    :paths '["src" "out/identicon"]'
}

release(){
  uberjar
}

"$@"