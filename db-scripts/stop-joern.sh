#!/bin/bash
JOERN_HOME=~/joern
NEO4J_HOME=~/neo4j-community-2.1.6

pushd .
cd /home/user/bugdooring_share/joern/

#echo "Stopping neo4j server"
sudo $NEO4J_HOME/bin/neo4j stop >>neo4j.log 2>&1

popd

