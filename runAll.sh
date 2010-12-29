#!/bin/bash

for SCHEME in "LDA" "CGS" "MWS" "MIR" "MPL" "ALC"
do
bash runEval.sh $SCHEME $1
done
