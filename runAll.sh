#!/bin/bash

# "ALC"
for SCHEME in  "MIR" "LDA" "MWS" "MPL" "CGS"
do
echo "running $SCHEME $1"
time bash runEval.sh $SCHEME $1 > $SCHEME-$1.runtime
done
