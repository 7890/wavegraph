#!/bin/bash

#needs ant, dot
#adds images to javadoc

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

ant
