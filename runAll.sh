#!/bin/bash

# for SCHEME in "LDA" "CGS" "MWS" "MIR" "MPL" "ALC"
for SCHEME in "LDA" "MIR"
do
bash runEval.sh $SCHEME $1
done
