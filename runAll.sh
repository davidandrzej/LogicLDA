#!/bin/bash

# "ALC"

# "s1MLAB" "s2CLAB" "macvspc"
for DATASET in "20news-allcomp" "congress"
do
    for SCHEME in "LDA" "CGS" "MWS" "MIR" "MPL"
    do
        echo "running $SCHEME $DATASET"
        time bash runEval.sh $SCHEME $DATASET > $SCHEME-$DATASET.runtime
    done
done

for DATASET in "polarity" "stewart"
do
    for SCHEME in "LDA" "MIR"
    do
        echo "running $SCHEME $DATASET"
        time bash runEval.sh $SCHEME $DATASET > $SCHEME-$DATASET.runtime
    done
done
