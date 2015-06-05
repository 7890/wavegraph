#!/bin/bash

#start wavgraph via swingexplorer to explore swing at runtime

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ x"$1" = x ]
then
	#find latest built jar automatically
	latest_jar="$DIR/../../build/"`ls -1tr "$DIR"/../../build | grep wavegraph | grep jar | tail -1`
else
	#use given jar on command line (full path needed)
	latest_jar="$1"
fi

if [ -e "$latest_jar" ]
then
	echo "found jar"
else
	echo "file not found: $latest_jar"
	exit 1
fi

echo "java -cp .:$DIR/swexpl.jar:$DIR/swag.jar:$latest_jar org.swingexplorer.Launcher Wavegraph $*"

java -cp .:"$DIR"/swexpl.jar:"$DIR"/swag.jar:"$latest_jar" org.swingexplorer.Launcher Wavegraph $*
