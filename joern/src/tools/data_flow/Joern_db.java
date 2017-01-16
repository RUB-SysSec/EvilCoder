package tools.data_flow;

//import joern.all.JoernSteps;

import neo4j.readWriteDB.Neo4JDBInterface;
//import neo4j.traversals.readWriteDB.Traversals;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.graphdb.index.*;
import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Relationship;
//import org.neo4j.graphdb.Direction;

import com.tinkerpop.gremlin.*;
import com.tinkerpop.gremlin.Tokens.*;
import com.tinkerpop.gremlin.java.*;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;



public class Joern_db
 {
 public static Neo4j2Graph g = null;
// public static Pipeline pipe = null;

  public static void initialize(String databaseDir)
   {
     if(g != null) return;

    Neo4JDBInterface.setDatabaseDir(databaseDir);
    Neo4JDBInterface.openDatabase();

    g = new Neo4j2Graph(Neo4JDBInterface.graphDb);
    Pipeline.g = g;

    System.out.println("initialized");
   }

  public static void initialize()
   {
    initialize("/home/user/bugdooring_share/joern/.joernIndex");
   }


  private static List<Node> IndexHits_to_List(IndexHits<Node> hits)
   {
   List<Node> ret = new ArrayList<Node>(); 
     for(Node n : hits)
      { 
       ret.add(n);
      } 
    hits.close(); 
    return ret;
   }

  private static List<Node> Neo4j2Vertices_to_Nodes(List<Neo4j2Vertex> vertices)
   {
   List<Node> ret = new ArrayList<Node>(); 
     for(Neo4j2Vertex v : vertices)
      { 
       ret.add(v.getRawVertex());
      } 
    return ret;
   }

  public static List<Node> queryNodeIndex(String query)
   {
   IndexHits<Node> result = Neo4JDBInterface.nodeIndex.query(query);
    return IndexHits_to_List(result);
   }

  public static List<Node> getNodesWithTypeAndName(String type, String name)
   {
   String query = "type:" + type + " AND name:" + name;
	return queryNodeIndex(query);
   }

  public static List<Node> getNodesWithTypeAndCode(String type, String code)
   {
   String query = "type:" + type + " AND code:" + code;
	return queryNodeIndex(query);
   }

  public static List<Node> getFunctionsByName(String name)
   {
    return getNodesWithTypeAndName("Function", name);
   }

  public static List<Node> getCallsTo(String name)
   {
   List<Node> callees = getNodesWithTypeAndCode("Callee", name);
//    return Neo4j2Vertices_to_Nodes(new Pipeline().start(callees).parents().toList());
    return new Pipeline().start(callees).parents().to_list();
   }





private static void print_class_name(Object o)
 {
  System.out.println(o.getClass().getName());
 }


// Find the first parent-id, which has an incoming reaches-edge
// Input: id of node in question
// Output:	node_id		<=> node has incoming REACHES-edge
//		-1		<=> node has no parent
//		recursive	<=> node has parents
//public static String find_reached_parent(Joern_db joern_db, String node_id, String edge_type="DATA_FLOW_EDGE", boolean in_edge = true) // REACHES
public static Long find_reached_parent(Long node_id, String edge_type, boolean in_edge) throws Exception // REACHES
 {
//  System.out.println(pipe.start(g.getVertex(Long.valueOf(117))).parents().to_list());
//  System.out.println(pipe.to_list());

 List<Node> e = null;
   if(in_edge)
    {
     e = Pipeline.v(node_id).in(edge_type).to_list();
    }
   else
    {
     e = Pipeline.v(node_id).out(edge_type).to_list();
    }

   if(e.size() > 0)
    {
     return node_id;
    }

// List<Node> p = joern_db.runGremlinQuery("g.v(" + node_id + ").parents()");
 List<Node> p = Pipeline.v(node_id).parents().to_list();
   if(p.size() == 0)
    {
     return new Long(-1);
    }
   else if(p.size() != 1)
    {
     throw new Exception("expected only one parent");
    }

 Object ret = p.get(0).getId();
  return find_reached_parent((Long)ret);
 }

public static Long find_reached_parent(Long node_id, String edge_type) throws Exception // REACHES
 {
  return find_reached_parent(node_id, edge_type, true); // REACHES
 }

public static Long find_reached_parent(Long node_id) throws Exception // REACHES
 {
  return find_reached_parent(node_id, "REACHES", true); // REACHES
 }

public static Long get_function_id(Long node_id)
 {
 List<Node> ret = Pipeline.v(node_id).to_list();
 Long function_id = (Long)(((Node)(ret.get(0))).getProperty("functionId"));
   if(function_id != null) return function_id;

  ret = Pipeline.v(node_id).parents().to_list();
 Long p_id = (Long)ret.get(0).getId();
  return get_function_id(p_id);
 }


// Find the ID of a function-definition, the function's name
// Assumes that the function's name is unique
public static Long get_function_id_by_name(String func_name)
 {
 List<Node> calls = getFunctionsByName(func_name);
  if(calls.size() != 1)
   {
    return new Long(-1);
//  throw new Exception("expected length 1");
   }
  return calls.get(0).getId();
 }


public static List<Long> get_function_ids_by_name(String func_name)
 {
 List<Node> calls = getFunctionsByName(func_name);
 List<Long> ids = new ArrayList<Long>();

   for(Node n : calls)
    { 
     ids.add(n.getId());
    } 

  return ids;
 }





// Returns all calls to a function, given the function's name.
// Simple frontend to the getCallsTo-Query.
public static List<Node> get_calls_to(String func_name)
 {
  return getCallsTo(func_name);
 }



public static void remove_edge_from_db(Long edge_id)
 {
  Pipeline.e(edge_id).remove();
 }


//def properties_to_gremlin_list(properties):
//	str_properties = "[";
//	for k in properties.keys():
//		str_properties += k + ": \"" + str(properties[k]).replace("\"", "\\\"") + "\", "
//	if(len(str_properties) > 1):
//		str_properties = str_properties[0:-2] + "]"
//	else:
//		str_properties += "]"
//
//	return str_properties


//def addNode(joern_db, properties):
//	str_properties = properties_to_gremlin_list(properties)
//	q = "g.addVertex(null, %s)" % str_properties
//#	print q
//	newNode = joern_db.runGremlinQuery(q)
//	return newNode


//def addRelationship(joern_db, src, dst, relType, properties):
//	if(len(properties) == 0):
//		q = "g.addEdge(null, g.v(%s), g.v(%s), '%s')" % (src, dst, relType)
//	else:
//		str_properties = properties_to_gremlin_list(properties)
//		q = "g.addEdge(null, g.v(%s), g.v(%s), '%s', %s)" % (src, dst, relType, str_properties)
//#	print q
//	joern_db.runGremlinQuery(q)



//def getNodeById(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
//	return node


//def getCalleeFromCall(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s).callToCallee()" % (node_id))
//	return node

//def getNodeType(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
//	return node["type"]
//
//def getNodeCode(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
//	return node["code"]

//def getOperatorCode(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
//	return node["operator"]

//def getNodeChildNum(joern_db, node_id):
//	node = joern_db.runGremlinQuery("g.v(%s)" % (node_id))
//	if("childNum" in node):
//		return int(node["childNum"])
//	else:
//		return 0

 } // EOF class

