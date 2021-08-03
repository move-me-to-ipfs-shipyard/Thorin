#!/bin/bash

repl(){
  clj \
    -X:repl deps-repl.core/process \
    :main-ns find.main \
    :port 7788 \
    :host '"0.0.0.0"' \
    :repl? true \
    :nrepl? false
}

main(){
  clojure \
    -J-Dclojure.core.async.pool-size=1 \
    -J-Dclojure.compiler.direct-linking=false \
    -M -m find.main
}

uberjar(){
  clojure \
    -X:uberjar hf.depstar/uberjar \
    :aot true \
    :jar out/find.standalone.jar \
    :verbose false \
    :main-class find.main
  mkdir -p out/jpackage-input
  mv out/find.standalone.jar out/jpackage-input/
}

j-package(){
  OS=${1:?"Need OS type (windows/linux/mac)"}

  echo "Starting build..."

  if [ "$OS" == "windows" ]; then
    J_ARG="--win-menu --win-dir-chooser --win-shortcut"
          
  elif [ "$OS" == "linux" ]; then
      J_ARG="--linux-shortcut"
  else
      J_ARG=""
  fi

  jpackage \
    --input out/jpackage-input \
    --dest out \
    --main-jar find.standalone.jar \
    --name "find" \
    --main-class clojure.main \
    --arguments -m \
    --arguments find.main \
    --app-version "1" \
    $J_ARG
  
}

"$@"