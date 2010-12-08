import numpy as NP

import sys
import os, os.path

basefn = sys.argv[1]
T = int(sys.argv[2])

vocab = [line.strip() for line in open('%s.vocab' % basefn)]
W = len(vocab)

alpha = float(50) / T
alphaf = open('%s.alpha' % basefn, 'w')
for ti in range(T):
    alphaf.write('%f ' % alpha)
alphaf.close()

beta = 0.01
betaf = open('%s.beta' % basefn, 'w')
for ti in range(T):
    for wi in range(W):
        betaf.write('%f ' % beta)
    betaf.write('\n')
betaf.close()

