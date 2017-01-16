#!/bin/sh

python init_glibc_data_trans.dat
time java -jar /home/user/joern/bin/init_glibc_data_trans.jar

time ./scripts/init-joern.sh /home/user/Desktop/data_flow/example_code/busybox/

time java -Xmx2048m -jar /home/user/joern/bin/taint_all.jar > taint_ergo.txt


sudo /home/user/neo4j-community-2.1.6/bin/neo4j stop

time java -Xmx2048m -jar /home/user/joern/bin/argumentTainter.jar ./generated_taint_all.txt /home/user/joern/.joernIndex/ > java_taint_ergo.txt
sudo /home/user/neo4j-community-2.1.6/bin/neo4j start


time java -Xmx2048m -jar /home/user/joern/bin/replace_member_edges.jar
time java -Xmx2048m -jar /home/user/joern/bin/remove_duplicated_edges.jar


time java -Xmx2048m -jar /home/user/joern/bin/find_data_paths.java 2> exceptions.txt 1> ergo.txt






