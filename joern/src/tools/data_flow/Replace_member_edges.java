package tools.data_flow;

//http://localhost:7474/db/data/node/30546/relationships/all/REACHES
//http://localhost:7474/db/data/relationship/43993

import java.util.ArrayList;
import java.util.List;


//import com.tinkerpop.blueprints.Vertex;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;

class Replace_member_edges
 {
  public static void replace_for_edge(Joern_db joern_db, Long edge_id)
   {
   List<Neo4j2Edge> ret = Pipeline.e(edge_id).toList();
   String before = ret.get(0).getProperty("var");
   String after = before.replaceAll("\\.", "->");

    System.out.print("replacing in edge ");
    System.out.println(edge_id);
    System.out.println("\t" + before);
    System.out.println("\t" + after);
//    joern_db.runGremlinQuery("g.e(%s).setProperty('var', '%s')" % (edge_id, after))
    joern_db.g.getEdge(edge_id).setProperty("var", after);

//    print joern_db.runGremlinQuery("g.e(%s).var" % edge_id)
   }


  public static List<Long> get_all_reaches_edges_with_member_access(Joern_db joern_db)
   {
   List<Long> edges = Pipeline.Vs().outE("REACHES").filter(
new PipeFunction<Neo4j2Edge,Boolean>()
 {
  public Boolean compute(Neo4j2Edge it)
   {
    String var = (String)(it.getProperty("var"));
    return var.indexOf('.') != -1;
   }
 }
).id().toList();
    return edges;

//   List<Long> ids = new ArrayList<Long>();
//    for(Neo4j2Edge e : edges)
//     {
//Long it = e.getId();
//      ids.add(it);
//     }
//    return ids;

//    edges = joern_db.runGremlinQuery("g.V.outE(DATA_FLOW_EDGE).has('var').filter{it.var.contains('.')}.id")
//    return edges
   }


  public static void main(String[] args) throws Exception
   {
   Joern_db joern_db = new Joern_db();
    joern_db.initialize();

   List<Long> edges = get_all_reaches_edges_with_member_access(joern_db);
    System.out.println(edges.size());

     for(Long e : edges)
      {
       replace_for_edge(joern_db, e);
      }

    edges = get_all_reaches_edges_with_member_access(joern_db);
    System.out.println(edges.size());

    joern_db.g.commit();
   }
} // EOF class

