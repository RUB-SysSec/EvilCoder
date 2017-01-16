# EvilCoder

This code accompanies the paper
*EvilCoder: Automated Bug Insertion*
(http://syssec.rub.de/research/publications/evilcoder/)
published as ACSAC'16.

In a nutshell, the task is to insert a (likely exploitable) bug into the
source code of a given project,
in order to improve bug-finding software by supplying it with examples of
how vulnerable code could look like.
Please have a look at the paper to get an overview for further details.

Most of the Java-code is actually a port from an earlier implementation in Python,
which is why it may look more like Python than Java and is
"integrated" with a flat-hierarchy.
Also, since it is not runtime-critical,
the last stage (instrumentation) was not ported and is therefore still in Python.


## Basics
We used a Debian Jessie for our setup, but any decent Linux should work.
The code depends on JOERN v0.2.5 (http://www.mlsec.org/joern/),
so please follow its excellent install guide.

## Compilation
Please copy the content of the */joern/*/ folder into your joern-installation.
In particular, this will overwrite some files,
so please make sure to save a version of your joern-directory.
In particular, it should change the *build.xml*-file, which contains the
ant-command to compile our added java-code, and modified sourcefiles.
For the most part, the changes should be marked.
Our code is in the folder *src/tools/data_flow/*.
Please note that the original argumentTainter-class was written by the authors
of Joern and only modified to accept a file for batch-processing.

In our setup, the following libraries have to be supplied in */joern/libs/*:
 * antlr4-annotations-4.2.1-SNAPSHOT.jar
 * antlr4-runtime-4.2.1-SNAPSHOT.jar
 * blueprints-core-2.5.0.jar
 * blueprints-neo4j2-graph-2.5.0.jar
 * commons-cli-1.2.jar
 * commons-configuration-1.6.jar
 * concurrentlinkedhashmap-lru-1.3.1.jar
 * geronimo-jta_1.1_spec-1.1.1.jar
 * gremlin-groovy-2.5.0.jar
 * gremlin-java-2.5.0.jar
 * groovy-1.8.9.jar
 * hamcrest-core-1.2.jar
 * junit.jar
 * lucene-core-3.6.2.jar
 * neo4j-cypher-2.1.6.jar
 * neo4j-cypher-commons-2.1.6.jar
 * neo4j-cypher-compiler-1.9-2.0.3.jar
 * neo4j-cypher-compiler-2.0-2.0.3.jar
 * neo4j-cypher-compiler-2.1-2.1.6.jar
 * neo4j-kernel-2.1.5.jar
 * neo4j-kernel-2.1.6.jar
 * neo4j-lucene-index-2.1.5.jar
 * neo4j-lucene-index-2.1.6.jar
 * neo4j-primitive-collections-2.1.5.jar
 * neo4j-primitive-collections-2.1.6.jar
 * parboiled-core-1.1.6.jar
 * parboiled-scala_2.10-1.1.6.jar
 * pipes-2.5.0.jar
 * scala-library-2.10.4.jar

After that, you should be able to compile the tools-folder.
In particular, you should find the following jar-files in */joern/bin/* afterwards:
 * init_glibc_data_trans.jar
 * taint_all.jar
 * argumentTainter.jar
 * replace_member_edges.jar
 * remove_duplicated_edges.jar
 * find_data_paths.jar

An example-script for how to run the sequence of tools is in *instr/_run.sh*.
For clarity, the steps are described here, as well.




## Input
As input, we expect the folder of a preprocessed C-project.
The scripts supplied in */preprocess/* should help to preprocess the files,
just like it would happen during the compilation of the project.
Some hand-tuning may be necessary, however, since compilation itself is often
non-trivial.


## Step 0. Initialiaze data-transfer for glibc
*Init_glibc_data_trans.java*
holds information on the data-transfer happening in well-known glibc-functions.
You have to run it to get a Java-serialized version of this dictionary used in
later stages.


## Step 1. Import the code-base into joern
Run */db-scripts/init-joern.sh <folder>*
See joern-manual for details.


## Step 2. Determine, which arguments influence one another
This is done by *taint_all.jar*.
Various data is generated at this point.
This includes a serialized version of the call-graph,
candidates for function-pointers
and the batch-file of modified function-arguments (generated_taint_all.txt).


## Step 3. Transfer this knowledge into the database
This step makes use of the (modified) argumentTainter.jar.
It marks function-arguments as modified on-call and adjusts the graph accordingly.


## (Step 3.5. Tidy up)
This step is not described in the paper. However, we noted some characteristic
errors in the graph after the aforementioned steps, so we run two small tidy-up
scripts (replace_member_edges and remove_duplicated_edges).


## Step 4. Find data-paths between user-controlled sources and sensitive sinks
This step tries to find data-paths from a sensitive sink,
meaning a certain function-argument of a certain sensitive function
(defined in Sensitive_sinks.java)
to a user-controlled source (defined in User_controlled_sources.java).

Should a data-path (an array of node-IDs) be found,
it is accompanied by an array of the variables,
which "overarch" these nodes (aka, the name of the variable in the data-transfer
for this step).

Next, the control-flows between the individual data-flow-nodes are found and printed, as well.

## Step 5. Instrumentation
As mentioned, this step was not reimplemented in Java and is therefore not
coupled tightly.
You can pick a random data-flow path and then a random control-flow
path inbetween those nodes.
Then, you invoke the Python-script *instrument.py* with pairs of a control-flow node-ID
and the corresponding overarching variable.

The script will first find all checks (i.e., if-conditions)
and then filter the relevant ones, i.e. for those,
which depend on the current overarched variable.

Next, it has to be determined, if the if- or the else-branch has to be executed
in order to allow a malicious data-flow.
A simple heuristic, based on which branch terminates the execution, is used here.

At this point, a possible instrumentation can be chosen.
Lacking a sophisticated code-checking and -manipulation DSL,
we used ad-hoc modification using Python.

An individual instrumenation is implemented as a Subclass of *Instrumentation*.
First and foremost, it has to analyze the check at hand,
to determine if it can be applied (*possible()*).
If so, it can be chosen at random to perform the instrumentation (*instrument*).
Should you implement your own instrumentations, 
don't forget to add it to *get_all_instrumentations()*.


# Contact
jannik DOT pewny AT rub DOT de)

