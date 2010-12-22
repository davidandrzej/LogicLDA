#!/bin/bash

SCHEME=$1
BASEFN=$2

KFOLD=5
NUMSAMP=2000
NUMOUTER=100
NUMINNER=100000
RANDSEED=194582

JARPATH=./target/logiclda-0.0.1-SNAPSHOT-jar-with-dependencies.jar
BASEDIR=$HOME/projects

java -Xmx2g -jar $JARPATH $BASEDIR/$BASEFN/$BASEFN $SCHEME $NUMSAMP $NUMOUTER $NUMINNER $RANDSEED $KFOLD