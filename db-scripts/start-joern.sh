#!/bin/bash
JOERN_HOME=~/joern
NEO4J_HOME=~/neo4j-community-2.1.6

pushd .
cd /home/user/bugdooring_share/joern/

sudo $NEO4J_HOME/bin/neo4j start >>neo4j.log 2>&1

popd

