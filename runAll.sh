#!/bin/bash

# for SCHEME in "LDA" "CGS" "MWS" "MIR" "MPL" "ALC"
for SCHEME in "LDA" "MIR"
do
echo "running $SCHEME $1"
time bash runEval.sh $SCHEME $1 > $SCHEME-$1.runtime
done
