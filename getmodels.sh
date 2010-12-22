#!/bin/bash

mkdir models
cd models
wget http://opennlp.sourceforge.net/models/english/tokenize/EnglishTok.bin.gz
wget http://opennlp.sourceforge.net/models/english/sentdetect/EnglishSD.bin.gz
wget http://jmlr.csail.mit.edu/papers/volume5/lewis04a/a11-smart-stop-list/english.stop
