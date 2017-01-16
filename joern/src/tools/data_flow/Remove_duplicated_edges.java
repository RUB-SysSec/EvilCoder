package tools.data_flow;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;

class Remove_duplicated_edges
 {
    public static void remove_edge_from_db(Joern_db joern_db, Long edge_id)
     {
//    print "removing edge", edge_id
//    e = joern_db.runGremlinQuery("g.e(%s)" % (edge_id))
//    print "Remove edge", e._id, ":", e.start_node._id, "->", e.end_node._id, ":", e.properties["var"]
      joern_db.g.getEdge(edge_id).remove();
     }


    public static void remove_edges_from_db(Joern_db joern_db, Iterable<Long> edge_ids)
     {
       for(Long edge_id : edge_ids)
        {
         remove_edge_from_db(joern_db, edge_id);
        }
     }


    static Boolean is_same_edge(Neo4j2Edge e1, Neo4j2Edge e2)
     {
     Neo4j2Vertex e1_s = e1.getProperty("start_node");
     Neo4j2Vertex e2_s = e2.getProperty("start_node");
       if((Long)e1_s.getId() != (Long)e2_s.getId()) return false;

     Neo4j2Vertex e1_e = e1.getProperty("end_node");
     Neo4j2Vertex e2_e = e2.getProperty("end_node");
       if((Long)e1_e.getId() != (Long)e2_e.getId()) return false;

       if(!e1.getProperty("type").equals(e2.getProperty("type"))) return false;

     Set<String> p1 = e1.getPropertyKeys();
     Set<String> p2 = e2.getPropertyKeys();
       if(p1.size() != p2.size()) return false;
       for(String it : p1)
        {
          if(!p2.contains(it)) return false;
          if(!e1.getProperty(it).equals(e1.getProperty(it))) return false;
        }

      return true;
     }


    static List<Neo4j2Edge> remove_duplicates(List<Neo4j2Edge> list)
     {
     ArrayList<Neo4j2Edge> result = new ArrayList<>();
     HashSet<Neo4j2Edge> set = new HashSet<>();

       for(Neo4j2Edge it : list)
        {
          if(!set.contains(it))
           {
            result.add(it);
            set.add(it);
           }
        }
      return result;
     }


   public static void remove_duplicated_edges_of_node(Joern_db joern_db, Long node_id)
    {
//    List<Neo4j2Edge> edges = joern_db.runGremlinQuery("g.v(%s).outE().gather{it}" % (node_id))
    List<Neo4j2Edge> edges = Pipeline.v(node_id).outE().toList(); // left out the gather...
      if(edges.size() == 0)
       {
        return;
       }
     edges = remove_duplicates(edges);
//    print edges

    HashSet<Long> edges_to_remove = new HashSet<>();
      for(int i = 0, i_end = edges.size(); i < i_end; ++i)
       {
//        print "edges[" + str(i) + "]: ", edges[i]._id
//        print "edges[" + str(i) + "] start: ", edges[i].start_node._id
//        print "edges[" + str(i) + "] end: ", edges[i].end_node._id
//        print "edges[" + str(i) + "] type: ", edges[i].type
//        print "edges[" + str(i) + "] properties: ", edges[i].properties

         for(int j = i+1, j_end = edges.size(); j < j_end; ++j)
          {
            if(is_same_edge(edges.get(i), edges.get(j)))
             {
//#                remove_edge[k] # run gremlin-query
//                print "I'd remove edge", edges[k]._id, "; equal to", edges[i]._id
//                print "Remove edge", edges[k]._id, ":", edges[k].start_node._id, "->", edges[k].end_node._id, ":", edges[k].properties["var"]
              edges_to_remove.add((Long)edges.get(j).getId());
             }
          }
      }

//    print "edges_to_remove:", edges_to_remove
    remove_edges_from_db(joern_db, edges_to_remove);
   }



//def remove_duplicated_edges(joern_db):
//    Ns = joern_db.runGremlinQuery("g.V.gather{it.id}")
//    Ns = Ns[0]
//
//    counter = 0
//    of = len(Ns)
//    for n in Ns:
//        counter += 1
//        print counter, "of", of
//        remove_duplicated_edges_of_node(joern_db, n)


   public static void remove_duplicated_edges_in_function(Joern_db joern_db, Long func_id)
    {
     System.out.print("remove_duplicated_edges_in_function: ");
     System.out.println(func_id);

    List<Node> Ns = Pipeline.v(func_id).functionToStatements().to_list();
//    Ns = Ns[0]

    Long counter = new Long(0);
    Long of = new Long(Ns.size());
      for(Node n : Ns)
       {
        counter += 1;
        System.out.println(counter.toString() + " of " + of.toString());
        remove_duplicated_edges_of_node(joern_db, n.getId());
       }
    }


  public static void main(String[] args) throws Exception
   {
     if(args.length != 1)
      {
       System.out.println("[/] Usage: <func_name>\n");
       System.exit(1);
      }
   String func_name = args[0];

   Joern_db joern_db = new Joern_db();
    joern_db.initialize();

   List<Node> hits = Joern_db.get_calls_to(func_name);
     for(Node h : hits)
      {
       remove_duplicated_edges_in_function(joern_db, (Long)h.getProperty("functionId"));
      }
//    remove_duplicated_edges();
    joern_db.g.commit();
   }
} // EOF class
