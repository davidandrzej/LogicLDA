"""
Aggregate results from grid-style experiments,
do some simple stat test
"""
import sys
import os, os.path
import pdb
import numpy as NP

# which dataset?
dataset = sys.argv[1]

#schemes = ['MIR','MPL','CGS','MWS','LDA','ALC']
#schemes = ['MIR','MPL','CGS','MWS','LDA']
schemes = ['MIR','LDA']
#schemes = ['MIR','MPL','MWS','LDA']


k = 5

# Populate arrays
schemevals = [[] for scheme in schemes]
for (si,scheme) in enumerate(schemes):
    for line in open(os.path.join(dataset, '%s-%s.cfv' % (dataset, scheme))):
        (lda, logic, logictotal) = line.split()
        schemevals[si].append(float(lda) + float(logic))

# Writeout table for R analysis
outf = open(os.path.join(dataset, '%s.rtable' % dataset), 'w')
for (si, vals) in enumerate(schemevals):
    for (ki, val) in enumerate(vals):
        outf.write('%f %d %d\n' % (val, ki, si))
outf.close()

# For each scheme, write out mean over folds
for (scheme, foldvals) in zip(schemes, schemevals):
    print '%s mean = %f' % (scheme, NP.array(foldvals).mean())
