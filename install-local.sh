#!/bin/bash

DFILE=-Dfile=./target/logiclda-0.0.1-SNAPSHOT-jar-with-dependencies.jar
DGROUP=-DgroupId=logiclda
DARTIFACT=-DartifactId=logiclda
DVERSION=-Dversion=0.0.1-SNAPSHOT
DPACKAGING=-Dpackaging=jar

mvn package
mvn install:install-file $DFILE $DGROUP $DARTIFACT $DVERSION $DPACKAGING
