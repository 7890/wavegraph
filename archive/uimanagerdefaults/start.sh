#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

#compile and create jar
#mkdir -p build
#javac -source 1.6 -target 1.6 -nowarn -d build UIManagerDefaults.java 
#cp Manifest.txt build
#cd build
#jar cfvm UIManagerDefaults.jar Manifest.txt *.class
#mv UIManagerDefaults.jar ..
#cd "$DIR"
#rm -rf build

java -jar UIManagerDefaults.jar &
