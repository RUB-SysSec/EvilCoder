package tools.data_flow;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;


class Get_call_graph
 {
// Compute the func-id call graph.
// Key: func-id
// Value: set of callee_func-id
  public static HashMap<Long, HashSet<Long>> get_func_id_call_graph(Joern_db joern_db)
   {
   HashMap<Long, HashSet<Long>> cg = new HashMap<>();
   List<Node> funcs = Joern_db.queryNodeIndex("type:Function");
   List<Long> func_ids = new ArrayList<>();
     for(Node f : funcs) func_ids.add(f.getId());
//   funcs = joern_db.runGremlinQuery("g.V.filter{it.type == 'Function'}.id")

     for(Long f : func_ids)
      {
      HashSet<Long> calls = new HashSet<>();
      List<Node> callees = Pipeline.v(f).functionToStatements().has("type", "CallExpression").callToCallee().to_list();

        for(Node callee : callees)
         {
         Long called_func_id = Joern_db.get_function_id_by_name((String)(callee.getProperty("code")));
           if(called_func_id != -1)
            {
             calls.add(called_func_id);
            }
         }

        if(!cg.containsKey(f))
         {
          cg.put(f, new HashSet<Long>());
         }

        for(Long c : calls)
         {
          cg.get(f).add(c);
         }
      }
    return cg;
   }


  private static String join(Collection<String> collection, String delimiter)
   {
   String joined = "";
   boolean first = true;
     for(String it : collection)
      {
       joined += it;
        if(first) first = false;
        else joined += delimiter;
      }
    return joined;
   }

// Compute the call graph.
// Key: (func_name, nof_params)
// Value: (func_sig, List of (callee_name, nof_args))
  public static HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> get_call_graph(Joern_db joern_db)
   {
   HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> cg = new HashMap<>();
//   funcs = joern_db.runGremlinQuery("g.V.filter{it.type == 'Function'}")
   List<Node> funcs = Joern_db.queryNodeIndex("type:Function");
     for(Long i = new Long(0), i_end = new Long(funcs.size()); i<i_end; ++i)
      {
      Node f = funcs.get(i.intValue());
      Long f_id = (Long)f.getId();
       System.out.println("handling func " + (String)f.getProperty("name") + "\t" + i.toString() + " of " + i_end.toString());

      List<Node> it = Pipeline.v(f_id).functionToAST().to_list();
      String func_sig = (String)(it.get(0).getProperty("code"));
      String func_name = (String)(f.getProperty("name"));
   
       it = Pipeline.v(f_id).functionToAST().children().has("type", "ParameterList").children().to_list();
      HashSet<Node> as_set = new HashSet<>(it);
      Long nof_params = new Long(as_set.size());
   
      HashSet<Pair<String, Long>> calls = new HashSet<>();
      List<Node> callees = Pipeline.v(f_id).functionToStatements().has("type", "CallExpression").callToCallee().to_list();
        for(Node callee : callees)
         {
         String callee_name = (String)(callee.getProperty("code"));
          it = Pipeline.v((Long)(callee.getId())).calleeToCall().callToArguments().to_list();
          as_set = new HashSet<>(it);
         Long nof_args = new Long(it.size());
   
          calls.add(new Pair<String, Long>(callee_name, nof_args));
         }
   
      Pair<String, Long> key = new Pair<>(func_name, nof_params);
        if(cg.containsKey(key))
         {
          System.out.println("func-collision for " + func_sig + " and " + cg.get(key).first);

         String now_func_sig = cg.get(key).first;
         HashSet<Pair<String, Long>> now_calls = cg.get(key).second;
         String[] names = now_func_sig.split("\n", -1);
         List<String> names_as_list = new ArrayList<>(Arrays.asList(names));
           if(!names_as_list.contains(func_sig))
            {
             names_as_list.add(func_sig);
             now_func_sig = join(names_as_list, "\n");
            }

           for(Pair<String, Long> c : calls)
            {
             now_calls.add(c);
            }
          cg.put(key, new Pair<String, HashSet<Pair<String, Long>>>(now_func_sig, now_calls));
         }
        else
         {
          cg.put(key, new Pair<String, HashSet<Pair<String, Long>>>(func_sig, calls));
         }
      }
    return cg;
   }

// Filter out func-signatures
  public static HashMap<Pair<String, Long>, HashSet<Pair<String, Long>>> get_simple_call_graph(HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> cg)
   {
   HashMap<Pair<String, Long>, HashSet<Pair<String, Long>>> simple = new HashMap<>();
     for(Pair<String, Long> k : cg.keySet())
      {
       simple.put(k, cg.get(k).second);
      }
    return simple;
   }

//## Filter out func-signatures and turn around the edges
//#def get_simple_dual_call_graph(cg):
//#   simple = dict()
//#   for from_func in cg.keys():
//#      for to_func in cg[from_func][1]:
//#         if(not to_func in simple):
//#            simple[to_func] = set()
//#         simple[to_func].add(from_func)
//#   return simple



// Helper-function to print a single entry of the call-graph
  public static void print_call_graph_entry(Pair<String, Long> cg_key, HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> cg)
   {
    System.out.println(cg_key.first + "\t(" + cg_key.second.toString() + " params)");
   Pair<String, HashSet<Pair<String, Long>>> val = cg.get(cg_key);

    System.out.println(val.first);
     for(Pair<String, Long> it : val.second)
      {
       System.out.println("\t->" + it.first + "\t(" + it.second.toString() + "params)");
      }
   }

// Helper-function to print the call-graph
  public static void print_call_graph(HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> cg)
   {
     for(Pair<String, Long> func : cg.keySet())
      {
       print_call_graph_entry(func, cg);
      }
   }

  public static void main(String[] args)
   {
   Joern_db joern_db = new Joern_db();
    joern_db.initialize();

   HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> cg = get_call_graph(joern_db);
    print_call_graph(cg);
   }

} // EOF class
