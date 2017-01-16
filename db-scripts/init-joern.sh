#!/bin/bash
JOERN_HOME=~/joern
NEO4J_HOME=~/neo4j-community-2.1.6


if [ "$#" -ne 1 ]; then
    echo "Usage: <full path to src>"
    exit
fi

pushd .
cd /home/user/bugdooring_share/joern/

#echo "Stopping neo4j server"
sudo $NEO4J_HOME/bin/neo4j stop >>neo4j.log 2>&1


rm -rf /home/user/bugdooring_share/joern/.joernIndex
java -jar $JOERN_HOME/bin/joern.jar "$1" | tee joern.log

sudo $NEO4J_HOME/bin/neo4j start >>neo4j.log 2>&1

popd

