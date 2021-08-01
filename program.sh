#!/bin/bash

repl(){
  clj -M:repl
}

main(){
  clojure -M:main
}

uberjar(){

  # clojure -X:depstar uberjar \
  #   :aot true \
  #   :jar target/find.standalone.jar \
  #   :verbose false \
  #   :main-class find.main


  lein with-profiles +prod uberjar
  mkdir -p target/jpackage-input
  mv target/find.standalone.jar target/jpackage-input/
  #  java -Dclojure.core.async.pool-size=1 -jar target/find-standalone.jar
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