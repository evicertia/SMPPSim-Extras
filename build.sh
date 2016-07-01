#!/bin/bash

SWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd "${SWD}"

mkdir -p build
javac -d build -cp "libs/*" AdvancedLifeCycleManager.java

pushd build
jar cvf ../smppsim-extras.jar *
popd

popd
