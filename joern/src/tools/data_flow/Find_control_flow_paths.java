package tools.data_flow;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;

import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

class Find_control_flow_paths
 {
    public static Boolean is_loop_free(List<Long> path)
     {
     // Helper-function for find_all_paths
     HashSet<Long> as_set = new HashSet<>(path);
      return path.size() == as_set.size();
     }


//   .loop("similar", new PipeFunction<LoopBundle<Vertex>, Boolean>() {
//                @Override
//                public Boolean compute(LoopBundle<Vertex> bundle) {
//                    return bundle.getLoops() < 4 && bundle.getObject() != v2;
//                }
//            }) 


//   List<Long> edges = Pipeline.Vs().outE("REACHES").property("var").filter(
//new PipeFunction<String,Boolean>()
// {
//  public Boolean compute(String it) {return it.indexOf('.') != -1;}
// }


// Find all paths of length <length> between two nodes, using only <edge_type> edges.
//    public static find_all_paths(joern_db, from_id, to_id, length=6, edge_type="CFG_EDGE"):
    public static List<List<Long>> find_all_paths(Joern_db joern_db, final Long from_id, final Long to_id, final Long length, String edge_type)
     {
//      s = "g.v({0:s}).out({3:s}).loop(1){{it.loops<={2:s} && !(it.object.id in [{0:s},{1:s}])}}.filter{{it.id=={1:s}}}.path"
//    s = s.format(str(from_id), str(to_id), str(length), edge_type)
     List<List<Neo4j2Vertex>> node_paths = Pipeline.v(from_id).out(edge_type).loop(1,
new PipeFunction<LoopBundle<Node>, Boolean>()
 {
   @Override
   public Boolean compute(LoopBundle<Node> bundle)
    {
    Long id = bundle.getObject().getId();
     return bundle.getLoops() < length && id != from_id && id != to_id;
    }
 }).has("id", to_id).path().toList();

    List<List<Long>> paths = new ArrayList<>();
      for(List<Neo4j2Vertex> p : node_paths)
       {
       List<Long> cur_path = new ArrayList<>();
         for(Neo4j2Vertex n : p)
          {
           cur_path.add(n.getRawVertex().getId());
          }
        paths.add(cur_path);
       }

     List<List<Long>> loop_free_paths = new ArrayList<>();
       for(List<Long> p : paths)
        {
          if(is_loop_free(p))
           {
            loop_free_paths.add(p);
           }
        }

      return paths;
     }

    public static List<List<Long>> find_all_paths(Joern_db joern_db, Long from_id, Long to_id)
     {
      return find_all_paths(joern_db, from_id, to_id, new Long(6), "CFG_EDGE");
     }

    public static List<List<Long>> find_all_paths(Joern_db joern_db, Long from_id, Long to_id, Long length)
     {
      return find_all_paths(joern_db, from_id, to_id, length, "CFG_EDGE");
     }

// Front-end to the "find_all_paths"-function.
// Calls it for a range of integers and returns the list of all found paths
    public static List<List<Long>> find_all_paths_in_range(Joern_db joern_db, Long from_id, Long to_id, Long min_length, Long max_length, String edge_type) throws Exception
     {
       if(min_length < 1) throw new Exception("min_length has to be >= 1");

     List<List<Long>> all_paths = new ArrayList<>();
       for(Long i = min_length; i<max_length; ++i)
        {
        List<List<Long>> paths = find_all_paths(joern_db, from_id, to_id, i, edge_type);
          for(List<Long> p : paths)
           {
            all_paths.add(p);
           }
        }
      return all_paths;
     }

    public static List<List<Long>> find_all_paths_in_range(Joern_db joern_db, Long from_id, Long to_id, Long min_length, Long max_length) throws Exception
     {
      return find_all_paths_in_range(joern_db, from_id, to_id, min_length, max_length, "CFG_EDGE");
     }

    public static List<List<Long>> find_all_paths_in_range(Joern_db joern_db, Long from_id, Long to_id, Long min_length) throws Exception
     {
      return find_all_paths_in_range(joern_db, from_id, to_id, min_length, new Long(6), "CFG_EDGE");
     }

    public static List<List<Long>> find_all_paths_in_range(Joern_db joern_db, Long from_id, Long to_id) throws Exception
     {
      return find_all_paths_in_range(joern_db, from_id, to_id, new Long(1), new Long(6), "CFG_EDGE");
     }


//def tree_like_path_append(all_paths, paths):
//    if(all_paths == []):
//        return paths[:]
//
//    new_paths = []
//    for a in all_paths:
//        for p in paths:
//            new_paths.append(a + p)
//    return new_paths

//# Find the indices in path, where path[i-1] == path[i]
    public static <T extends Comparable<T>> List<Long> find_double_element_indices(List<T> path)
     {
     List<Long> ret = new ArrayList<>();
      for(int i = 1, i_end = path.size(); i<i_end; ++i)
       {
         if(path.get(i-1).equals(path.get(i)))
          {
           ret.add(new Long(i));
          }
       }
      return ret;
     }

// Return a list, where each element from <the_list> with an index occurring in <indices> is removed.
// To that end, the indices are sorted and elements are removed from the end.
    public static <T extends Comparable<T>> List<T> remove_indices(List<T> the_list, List<Long> indices)
     {
     List<T> ret = new ArrayList(the_list);
      Collections.sort(indices);
       for(int i=indices.size()-1; i>=0; --i)
        {
         ret.remove(indices.get(i).intValue());
        }
      return ret;
     }


    public static <T extends Comparable<T>> List<T> remove_double_elements(List<T> path)
     {
     List<Long> indices = find_double_element_indices(path);
      return remove_indices(path, indices);
     }

// Collect the CFG-member-parents for each node in the path
// Assumption: The first node is run-time wise also the first
    public static List<Long> find_cfg_parents(Joern_db joern_db, List<Long> path) throws Exception
     {
     List<Long> cfg_parents = new ArrayList<>();
      cfg_parents.add(Joern_db.find_reached_parent(path.get(0), "CFG_EDGE", true));
       for(int i=1, i_end = path.size(); i<i_end; ++i)
        {
         cfg_parents.add(Joern_db.find_reached_parent(path.get(i), "CFG_EDGE", false));
        }
      return cfg_parents;
     }


    public static List<List<List<Long>>> find_all_cfg_connections(Joern_db joern_db, List<Long> cfg_parents, HashMap<Long, HashSet<Long>> func_id_call_graph) throws Exception
     {
    // Get the function-ids for each member in a path
     List<Long> function_ids = new ArrayList<>();
       for(Long cfg_parent : cfg_parents)
        {
         function_ids.add(Joern_db.get_function_id(cfg_parent));
        }
//    print "function_ids:", function_ids

//    print "func_id_call_graph:", func_id_call_graph

    List<List<List<Long>>> path_chain = new ArrayList<>();
    List<List<Long>> paths = new ArrayList<>();

      for(Long i = new Long(1), i_end = new Long(cfg_parents.size()); i<i_end; ++i)
       {
       Long from_index = i-1;
       Long to_index = i;
       Long from_id = cfg_parents.get(from_index.intValue());
       Long to_id = cfg_parents.get(to_index.intValue());
        System.out.println("finding paths for: " + from_id.toString() + "->" + to_id.toString());
         if(from_id == to_id)
          {
           throw new Exception("from_id == to_id... Should not happen");
          }
         else if(function_ids.get(from_index.intValue()) != function_ids.get(to_index.intValue()))
          {
//              print "function_jump", from_id, "->", to_id
          Boolean from_calls_to = func_id_call_graph.get(function_ids.get(from_index.intValue()).intValue()).contains(function_ids.get(to_index.intValue()));
          Boolean to_calls_from = func_id_call_graph.get(function_ids.get(to_index.intValue()).intValue()).contains(function_ids.get(from_index.intValue()));
//              print "from_calls_to:", from_calls_to
//              print "to_calls_from:", to_calls_from
  
            if(from_calls_to && to_calls_from)
             {
             // Note: In this case, there is a circular dependency.
             // This should happend mostly for recursive functions,
             // where there is no function-jump in the first place.
              throw new Exception("Cannot decide, which function called which");
             }
            else if(from_calls_to && !to_calls_from)
             {
             //print "from-func calls to-func => find CFG-Entry in to-func"
             Long cfg_entry_id = ((Node)(Pipeline.v(function_ids.get(to_index.intValue())).out("IS_FUNCTION_OF_CFG").toList().get(0))).getId();
//              print "cfg_entry_id:", cfg_entry_id
              paths = find_all_paths_in_range(joern_db, cfg_entry_id, to_id, new Long(1), new Long(30));
               for(int j=0, j_end=paths.size(); j<j_end; ++j)
                {
                 paths.get(j).remove(paths.get(j).size()-1);
                 paths.get(j).remove(0);
                }
             }
            else if(!from_calls_to && to_calls_from)
             {
             //print "to-func calls from-func => find CFG-Exits in from-func"
             List<Node> exit_nodes = Joern_db.queryNodeIndex("type:CFGExitNode AND functionId:" + function_ids.get(from_index.intValue()).toString());
               for(Node e : exit_nodes)
                {
                List<List<Long>> this_paths = find_all_paths_in_range(joern_db, from_id, e.getId(), new Long(1), new Long(30));
                  for(int j=0, j_end=this_paths.size(); j<j_end; ++j)
                   {
                    this_paths.get(j).remove(this_paths.get(j).size()-1);
                   }

                  for(List<Long> p : this_paths)
                   {
                    paths.add(p);
                   }
                }
             }
            else // no calls
             {
              throw new Exception("No calls found - should not happen");
             }
          }
         else
          {
           paths = find_all_paths_in_range(joern_db, from_id, to_id, new Long(1), new Long(30));
//          print "paths:", paths
//          all_paths = tree_like_path_append(all_paths, paths)
          }

         for(int j=0, j_end=paths.size(); j<j_end; ++j)
          {
           paths.set(j, remove_double_elements(paths.get(j)));
          }

         if(paths.size() == 0)
          {
           System.out.println("COULD NOT FIND PATH");
          }

        path_chain.add(paths);
//          print "all_paths:", all_paths
//      all_paths = map(lambda p: remove_double_elements(p), all_paths)
       }
     return path_chain;
    }



    public static List<String> find_var_name_overarch(List<Long> path, List<Long> cfg_parents, List<String> var_names)
     {
     List<String> overarch = new ArrayList<>();
     int data_index = 1;
     int cfg_index = 1;

       while(cfg_index < path.size())
        {
          while(path.get(cfg_index) != cfg_parents.get(data_index))
           {
            overarch.add(var_names.get(data_index-1));
            cfg_index += 1;
           }
         overarch.add(var_names.get(data_index-1));
         data_index += 1;
         cfg_index += 1;
        }
      return overarch;
     }


    public static Long count_paths(List<List<List<Long>>> path_chain)
     {
     Long counter = new Long(1);
       for(List<List<Long>> paths : path_chain)
        {
         counter *= paths.size();
        }
      return counter;
     }


    public static List<Long> get_ith_path(List<List<List<Long>>> path_chain, int ith_path)
     {
     List<Long> place_values = new ArrayList<>();
      place_values.add(new Long(1));

       for(int i=1, i_end=path_chain.size(); i<i_end; ++i)
        {
        int from_behind = path_chain.size()-1 - i;
         place_values.add(new Long(place_values.get(i-1) * path_chain.get(from_behind).size()));
        }

      Collections.reverse(place_values);

     List<Long> indices = new ArrayList<>(path_chain.size());
     Long counter = new Long(ith_path);
       for(int i=0, i_end=path_chain.size(); i<i_end; ++i)
        {
          if(counter >= place_values.get(i))
           {
           Long modulo = counter % place_values.get(i);
           Long divisible = (counter - modulo);
            indices.set(i, indices.get(i) + divisible  / place_values.get(i));
            counter = modulo;
           }
        }

     List<Long> path = new ArrayList<>();
      for(int i =0, i_end=indices.size(); i<i_end; ++i)
       {
         for(Long node : path_chain.get(i).get(indices.get(i).intValue()))
          {
           path.add(node);
          }
       }

      path = remove_double_elements(path);
      return path;
     }


    private static List<Long> to_list(int... args)
     {
     List<Long> ret = new ArrayList<>();
       for(int arg : args)
        {
         ret.add(new Long(arg));
        }
      return ret;
     }


    public static void get_ith_path()
     {
     List<List<Long>> one_two = new ArrayList<>();
      one_two.add(to_list(1));
      one_two.add(to_list(2));


     List<List<Long>> three_four_five = new ArrayList<>();
      three_four_five.add(to_list(3));
      three_four_five.add(to_list(4));
      three_four_five.add(to_list(5));

     List<List<List<Long>>> path_chain = new ArrayList<>();
      path_chain.add(one_two);
      path_chain.add(three_four_five);
 
     Long chain_len = count_paths(path_chain);
      System.out.println("chain_len: " + chain_len.toString());
      assert 6 == chain_len;
 
     List<Long> path = null;
      path = get_ith_path(path_chain, 0);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(1, 3));
 
      path = get_ith_path(path_chain, 1);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(1, 4));
  
      path = get_ith_path(path_chain, 2);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(1, 5));
  
      path = get_ith_path(path_chain, 3);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(2, 3));
  
      path = get_ith_path(path_chain, 4);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(2, 4));
  
      path = get_ith_path(path_chain, 5);
      System.out.println("path: " + path.toString());
      assert path.equals(to_list(2, 5));
     }
 } // EOF class

