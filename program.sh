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
  clojure -M:main
}

uberjar(){

  mkdir -p target/jpackage-input
  mv target/find.standalone.jar target/jpackage-input/
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

  APP_VERSION=0.1.0

  jpackage \
    --input target/jpackage-input \
    --dest target \
    --main-jar find.standalone.jar \
    --name "find" \
    --main-class clojure.main \
    --arguments -m \
    --arguments find.main \
    --resource-dir resources \
    --java-options -Xmx2048m \
    --app-version ${APP_VERSION} \
    $J_ARG
  
}

"$@"