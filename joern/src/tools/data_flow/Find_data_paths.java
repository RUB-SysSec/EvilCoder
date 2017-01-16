package tools.data_flow;

import java.io.File;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Collections;

import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe.LoopBundle;

import java.lang.Iterable;


class Ergo_item
 {
   public String task_id;
   public List<Long> source;
   public List<Object> var_names;

   public Ergo_item(String task_id, List<Long> source, List<Object> var_names)
    {
     this.task_id = task_id;
     this.source = source;
     this.var_names = var_names;
    }

   @Override
   public String toString()
    {
    String s = "";
     s += "task_id: " + task_id + "\n";
     s += "\tsource: " + source.toString() + "\n";
     s += "\tvar_names: " + var_names.toString() + "\n";
     return s;
    }

   @Override
   public int hashCode()
    {
    int hash = 1;
     hash = 31 * hash + task_id.hashCode();
     hash = 31 * hash + source.hashCode();
     hash = 31 * hash + var_names.hashCode();
     return hash;
    }
 
   @Override
   public boolean equals(Object obj)
    {
      if(!(obj instanceof Ergo_item)) return false;
      if(this == obj) return true;
    Ergo_item other = (Ergo_item)obj;
      if(!task_id.equals(other.task_id)) return false;
      if(!source.equals(other.source)) return false;
      if(!var_names.equals(other.var_names)) return false;
     return true;
    }
 }


class Had_already
 {
   public Long cur_id;
   public String var_name;
   public Boolean continue_at_self;
   public List<Long> call_stack;

   public Had_already(Long cur_id, String var_name, Boolean continue_at_self, List<Long> call_stack)
    {
     this.cur_id = cur_id;
     this.var_name = var_name;
     this.continue_at_self = continue_at_self;
     this.call_stack = new ArrayList<>(call_stack);
    }

   @Override
   public int hashCode()
    {
    int hash = 1;
     hash = 31 * hash + cur_id.hashCode();
     hash = 31 * hash + var_name.hashCode();
     hash = 31 * hash + continue_at_self.hashCode();
     hash = 31 * hash + call_stack.hashCode();
     return hash;
    }
 
   @Override
   public boolean equals(Object obj)
    {
      if(!(obj instanceof Had_already)) return false;
      if(this == obj) return true;
    Had_already other = (Had_already)obj;
      if(cur_id != other.cur_id) return false;
      if(!var_name.equals(other.var_name)) return false;
      if(continue_at_self != other.continue_at_self) return false;
      if(!call_stack.equals(other.call_stack)) return false;
     return true;
    }
 }







public class Find_data_paths
 {
  public static HashMap<String, HashMap<Long, List<Long>>> sets_param_dict = null;

  static
   {
    sets_param_dict = (HashMap<String, HashMap<Long, List<Long>>>)Pickle.load_from_file("sets_param_dict.ser");
   }


  public static String member_to_var_name (Object var_name)
   {
     if(var_name instanceof Iterable)
      {
      List<String> casted = (List<String>)(var_name);
      String s = "";
        for(int i=0, i_end=casted.size(); i<i_end; ++i)
         {
           if(i > 0)
            {
             s += " -> ";
            }
          s += casted.get(i).replaceAll("* ", "");
         }
       return s;
      }
    else
     {
      return (String)(var_name);
     }
   }


// Return the <ith_argument> of a CallExpression <node_id>
  public static Node get_argument_i(Joern_db joern_db, Long node_id, Long ith_argument) throws Exception
   {
   List<Node> it = Pipeline.v(node_id).callToArguments().has("childNum", ith_argument.toString()).to_list();
     if(it.size() != 1)
      {
       throw new Exception("Expected length 1: " + node_id.toString() + " " + ith_argument.toString());
      }
    return it.get(0);
   }



// Returns the nodes, which have data-flow to <node_id> for <var_name>
  public static List<Node> get_reached_by(Joern_db joern_db, Long node_id, Object var_name)
   {
   System.out.println("get_reached_by, node_id: " + node_id.toString());
   List<Node> defs = Pipeline.v(node_id).inE("REACHES").has("var", member_to_var_name(var_name)).outV().to_list();
    return defs;
   }




// TODO: merge with logic in taint.py?
// Decides, if a var_name is defined, because it is on the Left-Hand-Side.
// Works for IdentifierDeclStatement (char *buf = other_buf;)
// and ExpressionStatement (buf = other_buf)
  public static Boolean is_lhs_of_assignment(Joern_db joern_db, Long node_id, Object var_name)
   {
    // if node is IdentifierDeclStatement
   List<Node> via_ids = Pipeline.v(node_id).children().has("type", "IdentifierDecl").children().has("type", "Identifier").has("code", member_to_var_name(var_name)).to_list();

     if(via_ids.size() != 0)
      {
       return true;
      }

    // if node is ExpressionStatement
   List<Node> via_es = Pipeline.v(node_id).children().has("type", "AssignmentExpr").lval().has("code", member_to_var_name(var_name)).to_list();
     if(via_es.size() != 0)
      {
       return true;
      }

    return false;
   }



// TODO: works for "buf" or "(char *) & len", but not for "buf + len" (only buf!)
  public static Boolean is_user_controlled_arg(Joern_db joern_db, Long node_id, Object var_name) throws Exception
   {
   Node node = (Node)(Pipeline.v(node_id).to_list().get(0));
    if(((String)(node.getProperty("type"))).equals("Parameter"))
     {
     Node func = (Node)(Pipeline.v(node_id).parents().parents().to_list().get(0));
     String func_code = (String)(func.getProperty("code"));
       if(func_code.startsWith("main "))
        {
         return true;
        }
     }

   Long call_ex_id = null;
     if(((String)(node.getProperty("type"))).equals("Argument"))
      {
       call_ex_id = ((Node)(Pipeline.v(node_id).parents().parents().to_list().get(0))).getId();
      }
     else
      {
      List<Node> ns = Pipeline.v(node_id).children().has("type", "CallExpression").to_list();
        if(ns.size() == 0)
         {
          return false;
         }
       call_ex_id = ns.get(0).getId();
      }

   List<Node> callees = Pipeline.v(call_ex_id).callToCallee().to_list();
     if(callees.size() == 0)
      {
       return false;
      }
 
     if(callees.size() != 1)
      {
       throw new Exception("expected one callee");
      }
   Node callee = callees.get(0);

     if(User_controlled_sources.func_is_user_controlled((String)callee.getProperty("code")))
      {
      List<Long> user_controlled_args = User_controlled_sources.get_user_controlled_args((String)callee.getProperty("code"));
        for(Long u : user_controlled_args)
         {
         List<Node> arg_u = Pipeline.v(call_ex_id).callToArguments().has("childNum", u.toString()).to_list();
         String arg_u_code = " " + (String)(arg_u.get(0).getProperty("code")) + " ";
//            print "arg_u_code:", arg_u_code
            if(arg_u_code.contains(" " + var_name + " "))
             {
              return true;
             }
          }
       }

    return false;
   }





//def find_inflow_nodes_with_path_length(joern_db, node_id, path_length, nodes_in_step_before = set([])):
  public static HashSet<Long> find_inflow_nodes_with_path_length(Joern_db joern_db, Long node_id, Long path_length, HashSet<Long> nodes_in_step_before)
   {
   ArrayList<Long> start_nodes = new ArrayList(nodes_in_step_before);
     if(!nodes_in_step_before.contains(node_id))
      {
       start_nodes.add(node_id);
      }
   List<Node> nodes = Pipeline.v(start_nodes).in("CFG_EDGE").to_list();

   HashSet<Long> new_node_ids = new HashSet();
     for(Node n : nodes)
      {
      Long id = n.getId();
        if(!nodes_in_step_before.contains(id))
         {
          new_node_ids.add(id);
         }
      }
    return new_node_ids;
   }

  public static HashSet<Long> find_inflow_nodes_with_path_length(Joern_db joern_db, Long node_id, Long path_length)
   {
    return find_inflow_nodes_with_path_length(joern_db, node_id, path_length, new HashSet<Long>());
   }


// if a child uses the var, take that
  public static Long get_deepest_child_using(Joern_db joern_db, Long node_id, String var_name_together)
   {
   Long deepest = new Long(node_id);
   Long before = new Long(node_id);
     while(true)
      {
      Set<Long> childs = Function_sets_parameter.get_all_children(joern_db, deepest);
//        print "childs:", childs
      Boolean found_other = false;
        for(Long c : childs)
         {
         List<Node> uses = Pipeline.v(c).uses().to_list();
           for(Node u : uses)
            {
            String code = (String)(u.getProperty("code"));
            String u_together = member_to_var_name(Function_sets_parameter.split_into_elements(code));
              if(u_together.equals(var_name_together))
               {
//                  print "new deepest:", c
                deepest = c;
                found_other = true;
                break;
               }
              if(found_other)
               {
                break;
               }
            }
         }
        if(deepest == before)
         {
          break;
         }
        before = deepest;
      }
    return deepest;
   }


// node_id is not reached by any REACHES-edges labeled "var_name".
// So, try to find a node with cfg-flow to this node, which uses the var_name
  public static List<Node> get_cfg_same_use(Joern_db joern_db, Long node_id, Object var_name)
   {
   String var_name_together = member_to_var_name(var_name);
   List<Long> defs = new ArrayList<>();
   HashSet<Long> nodes_till_now = new HashSet<>();
     for(Long i = new Long(1), i_end = new Long(50); i<i_end; ++i)
      {
//        print "i:", i
      HashSet<Long> nodes = find_inflow_nodes_with_path_length(joern_db, node_id, i, nodes_till_now);
        for(Long n : nodes)
         {
         List<Node> uses = Pipeline.v(n).uses().to_list();
           for(Node u : uses)
            {
            String code = (String)(u.getProperty("code"));
            String u_together = member_to_var_name(Function_sets_parameter.split_into_elements(code));
              if(u_together.equals(var_name_together))
               {
//                print "get_cfg_same_use, found match:", n
                defs.add(get_deepest_child_using(joern_db, n, var_name_together));
               }
            }
         }
//      print "n, uses:", n, uses
        if(defs.size() != 0)
         {
          break;
         }
       nodes_till_now = nodes;
      }
    
    return Pipeline.v(defs).to_list();
   }


  public static Boolean matches_expect(Location loc, Location expected)
   {
   File f = new File(loc.file_name);
   String file_name = f.getName(); // remove the path-name
    if(!file_name.equals(expected.file_name))
      return false;
    if(!loc.function_name.equals(expected.function_name))
      return false;
    if(loc.line_no != expected.line_no)
      return false;
    return true;
   }

// Computes the "reached_by"-tree for a node_id and a var_name.
// Each node in the tree contains: (node_id, this_nodes_defs)
// Able to decide, whether a node defines by "lhs" or "tainted_arg".
// Aborts, when it finds a "user_controlled_def".
// Uses a Breadth-First-Search.
  public static void get_defs_of(Joern_db joern_db, Long node_id, String initial_var_name, String task_id, Set<Ergo_item> ergo_set, Set<Had_already> had_already)
   {
   Long matched = new Long(0);
// TEST
   List<Location> expect = new ArrayList<>();
    expect.add(new Location("pngrio.c", "png_default_read_data", 2990)); // check = (png_size_t)fread(data, (png_size_t)1, length, (png_FILE_p)png_ptr->io_ptr);
    expect.add(new Location("pngrio.c", "png_default_read_data", 2985)); //png_default_read_data(png_structp png_ptr, png_bytep data, png_size_t length)
    expect.add(new Location("pngrio.c", "png_read_data", 2980)); // (*(png_ptr->read_data_fn))(png_ptr, data, length);
    expect.add(new Location("pngrio.c", "png_read_data", 2976)); // param
    expect.add(new Location("pngrutil.c", "png_crc_read", 3030));   // calling png_read_data(png_ptr, buf, length);
    expect.add(new Location("pngrutil.c", "png_crc_read", 3026)); // param

    expect.add(new Location("pngrutil.c", "png_handle_tRNS", 3861)); // calling png_crc_read
    expect.add(new Location("pngrutil.c", "png_handle_tRNS", 3796)); // the param
    expect.add(new Location("pngpread.c", "png_push_read_chunk", 3217)); // calling handle_tRNS
    expect.add(new Location("pngpread.c", "png_push_read_chunk", 3071)); // png_ptr->push_length = png_get_uint_31(png_ptr, chunklength)
    expect.add(new Location("pngrutil.c", "png_get_uint_31", 2978));
    expect.add(new Location("pngpread.c", "png_push_read_chunk", 3071)); // via buf from png_get_uint_31
    expect.add(new Location("pngpread.c", "png_push_read_chunk", 3070)); // via chunklength from png_get_uint_31
    expect.add(new Location("pngpread.c", "png_push_fill_buffer", 3371)); // memcpy, leading to png_ptr->save_buffer_ptr
    expect.add(new Location("pngpread.c", "png_push_fill_buffer", 3358)); // parameter via png_ptr->save_buffer_ptr
    expect.add(new Location("pngpread.c", "png_push_read_chunk", 3060)); // parameter, via png_ptr
    expect.add(new Location("pngpread.c", "png_process_some_data", 3001)); // callsite of png_push_read_chunk
    expect.add(new Location("pngpread.c", "png_process_some_data", 2988)); // parameter, via png_ptr
    expect.add(new Location("pngpread.c", "png_process_data", 2984)); // callsite of png_process_some_data
    expect.add(new Location("pngpread.c", "png_process_data", 2981)); // calling png_push_restore_buffer
    expect.add(new Location("pngpread.c", "png_push_restore_buffer", 3449)); // png_ptr->current_buffer_ptr = png_ptr->current_buffer;
    expect.add(new Location("pngpread.c", "png_push_restore_buffer", 3446)); // png_ptr->current_buffer = buffer;
    expect.add(new Location("pngpread.c", "png_push_restore_buffer", 3443)); //param buffer;
    expect.add(new Location("pngpread.c", "png_process_data", 2981)); // callsite of png_push_restore_buffer
    expect.add(new Location("pngpread.c", "png_process_data", 2977)); // param buffer
    expect.add(new Location("readpng2.c", "readpng2_decode_data", 2551)); // callsite of png_process_data
    expect.add(new Location("readpng2.c", "readpng2_decode_data", 2541)); // param rawbuf
    expect.add(new Location("rpng2-x.c", "main", 6223)); // callsite of readpng2_decode_data; arg-name: inbuf
    expect.add(new Location("rpng2-x.c", "main", 6165)); // incount = fread(inbuf, 1, 4096, infile);
//  List<Location> expect = new ArrayList<>(); // NOTE: uncomment for single experiment

   HashMap<Long, HashSet<String>> func_ptr_candidates = (HashMap<Long, HashSet<String>>)Pickle.load_from_file("func_ptr_candidates.ser");
   HashMap<String, List<Long>> func_ptr_names = (HashMap<String, List<Long>>)Pickle.load_from_file("func_ptr_names.ser");
//    print "func_ptr_candidates:", func_ptr_candidates
//    print "func_ptr_names:", func_ptr_names
//    System.exit(1);

   HashMap<String, Set<Long>> func_ptr_reverse_candidates = new HashMap<>();
     for(Long func_ptr_callee : func_ptr_candidates.keySet())
      {
        for(String func_name_use : func_ptr_candidates.get(func_ptr_callee))
         {
           if(!func_ptr_reverse_candidates.containsKey(func_name_use))
            {
             func_ptr_reverse_candidates.put(func_name_use, new HashSet<Long>());
            }
          func_ptr_reverse_candidates.get(func_name_use).add(func_ptr_callee);
         }
      }




//    had_already = set()
   Def_tree def_tree = new Def_tree(node_id, initial_var_name, null);
   LinkedList<Def_tree> q = new LinkedList<>();
    q.add(def_tree);
    while(q.size() > 0)
     {
        try
         {
          System.out.println("queue.size(): " + (new Long(q.size())).toString());
          Def_tree q_def = q.pollFirst(); // to make it Breadth-First-Search
          Long cur_id = q_def.node_id;
          Object var_name = q_def.var_name;
          Boolean continue_at_self = q_def.continue_at_self;
          List<Long> call_stack = q_def.call_stack;

          Location loc_tuple = Location.get_location_tuple(joern_db, cur_id);
           System.out.println("loc_tuple: " + loc_tuple.toString());
//System.out.println("HERE A"); System.out.flush();
            if(expect.size() > 0 && matches_expect(loc_tuple, expect.get(0)))
             {
              matched += 1;
             // remove the current element from expect
              expect.remove(0);
              System.out.println("Matches an expected node, remove all " + (new Long(q.size()-1)).toString() + " other alternatives; matched: " + matched.toString() + " with " + (new Long(expect.size())).toString() + " left");
             //remove all others from q
              q = new LinkedList<>();
             }

           System.out.println("\n\n");
           System.out.println("cur_id: " + cur_id.toString());
           System.out.println("var_name: " + var_name);
           System.out.println("continue_at_self: " + continue_at_self.toString());
           System.out.println("call_stack: " + call_stack.toString());

//           Had_already had_this = new Had_already(cur_id, member_to_var_name(var_name), continue_at_self, call_stack);
           Had_already had_this = new Had_already(cur_id, member_to_var_name(var_name), continue_at_self, new ArrayList<Long>());
             if(had_already.contains(had_this))
              {
               System.out.println("had_already");
               continue;
              }
             else
              {
System.out.print("had_already.size(): ");
System.out.println(had_already.size());
               had_already.add(had_this);
              }
//            if((cur_id, member_to_var_name(var_name), continue_at_self, tuple(call_stack)) in had_already):
//            if((cur_id, member_to_var_name(var_name), continue_at_self) in had_already):
//                print "had already"
//                continue
//            else:
//                had_already.add((cur_id, member_to_var_name(var_name), continue_at_self, tuple(call_stack)))
//                had_already.add((cur_id, member_to_var_name(var_name), continue_at_self))
//                had_already[(cur_id, member_to_var_name(var_name), continue_at_self)] = 1
    
          Long reached_parent = Joern_db.find_reached_parent(cur_id);
            if(continue_at_self && reached_parent == -1)
             {
              reached_parent = cur_id;
             }
           System.out.println("reached_parent: " + reached_parent.toString());
            if(reached_parent == -1)
             {
              continue;
             }
    
          List<Node> defs = null;
            if(continue_at_self)
             {
              defs = Pipeline.v(reached_parent).to_list();
             }
            else
             {
              defs = get_reached_by(joern_db, reached_parent, var_name);
               if(defs.size() == 0)
                {
                 System.out.println("nothing reached it... but has to. use CFG-fallback");
                 defs = get_cfg_same_use(joern_db, reached_parent, var_name);
                }
             }
    
    
    
            for(int i=0, i_end=defs.size(); i<i_end; ++i)
             {
              loc_tuple = Location.get_location_tuple(joern_db, defs.get(i).getId());
              System.out.println("loc_tuple: " + loc_tuple.toString());
               if(expect.size() > 0 && matches_expect(loc_tuple, expect.get(0)))
                {
                 matched += 1;
                // remove the current element from expect
                 expect.remove(0);
                 System.out.println("Matches an expected node, remove all " + (new Long(q.size()-1)).toString() + " other alternatives; matched: " + matched.toString() + " with " + (new Long(expect.size())).toString() + " left");
                //remove all others from defs
                Node the_def = defs.get(i);
                 defs = new ArrayList<>();
                 defs.add(the_def);
                //remove all others from q
                 q = new LinkedList<>();
                 break;
                }
             }
    
    
    
            for(Node d : defs)
             {
              System.out.println("\n");
             Def_tree cur = new Def_tree(d.getId(), var_name, q_def, false, call_stack);
              q_def.add_sub_tree(cur);
              System.out.println("in def: " + d.toString());
    
              System.out.println("var_name: " + var_name.toString());
             Boolean is_uca = is_user_controlled_arg(joern_db, d.getId(), var_name);
              System.out.println("user_controlled_arg: " + is_uca.toString());
               if(is_uca)
                {
                Pair<List<Long>, List<Object>> dummy_pair = retrace_source_and_var_names(cur);
                List<Long> ucs_source = dummy_pair.first;
                List<Object> ucs_var_names = dummy_pair.second;
                 System.out.println("FOUND:");
                 System.out.println("ucs_source: " + ucs_source.toString());
                 System.out.println("ucs_var_names: " + ucs_var_names.toString());
                 ergo_set.add(new Ergo_item(task_id, ucs_source, ucs_var_names));
                 continue;
                }
    
             Boolean is_ret = ((String)(d.getProperty("type"))).equals("ReturnStatement");
              System.out.println("is_ret: " + is_ret.toString());
               if(is_ret)
                {
                List<Node> in_vars_nodes = Pipeline.v(d.getId()).inE("REACHES").to_list();
                Set<String> in_vars = new HashSet<>();
                  for(Node in_var_node : in_vars_nodes)
                   {
                    in_vars.add((String)(in_var_node.getProperty("var")));
                   }
                 System.out.println("in_vars: " + in_vars.toString());
                  for(String v : in_vars)
                   {
                   Def_tree v_cur = new Def_tree(d.getId(), v, q_def, false, call_stack);
                    q.add(v_cur);
                   }
    
                // ReturnStatement -> CallExpression -> Callee
                List<Node> callees = Pipeline.v(d.getId()).children().has("type", "CallExpression").children().has("type", "Callee").to_list();
    
                  for(Node c : callees)
                   {
                   String func_name = (String)(c.getProperty("code"));
                     if(User_controlled_sources.arg_is_user_controlled(func_name, new Long(-1)))
                      {
                      List<Long> new_callstack = new ArrayList<>(call_stack);
                       new_callstack.add(c.getId());
                      Def_tree ret = new Def_tree(c.getId(), "", cur, false, new_callstack);
                       cur.add_sub_tree(ret);

                      Pair<List<Long>, List<Object>> dummy_pair = retrace_source_and_var_names(ret);
                      List<Long> ucs_source = dummy_pair.first;
                      List<Object> ucs_var_names = dummy_pair.second;
                       System.out.println("FOUND:");
                       System.out.println("ucs_source: " + ucs_source.toString());
                       System.out.println("ucs_var_names: " + ucs_var_names.toString());
                       ergo_set.add(new Ergo_item(task_id, ucs_source, ucs_var_names));
                       continue;
                      }
                     else
                      {
                      Long func_id = Joern_db.get_function_id_by_name(func_name);
                       System.out.println("check retval of function " + func_id.toString() + " <- " + func_name);
                      List<Long> returns = Function_sets_parameter.retval_of_func(joern_db, func_id);
                        for(Long r : returns)
                         {
                         List<Long> new_callstack = new ArrayList<>(call_stack);
                          new_callstack.add(c.getId());
                         Def_tree ret = new Def_tree(r, "", cur, true, new_callstack);
                          cur.add_sub_tree(ret);
                          q.add(ret);
                         }
                      }
                   }
                 continue;
                }
    
    
             Boolean is_lhs = is_lhs_of_assignment(joern_db, d.getId(), var_name);
              System.out.println("is_lhs: " + is_lhs.toString());
               if(is_lhs)
                {
                List<Node> in_vars_nodes = Pipeline.v(d.getId()).inE("REACHES").to_list();
                Set<String> in_vars = new HashSet<>();
                  for(Node in_var_node : in_vars_nodes)
                   {
                    in_vars.add((String)(in_var_node.getProperty("var"))); // NOTE: the original code made a set from in_var_nodes.get(0)...
                   }
                 System.out.println("in_vars: " + in_vars.toString());

                  for(String v : in_vars)
                   {
                   Def_tree v_cur = new Def_tree(d.getId(), v, q_def, false, call_stack);
                    q.add(v_cur);
                   }


                    // for func-calls... check ret-val
                List<Node> callees = new ArrayList<>();
                  if(((String)(d.getProperty("type"))).equals("IdentifierDeclStatement"))
                   {
                   // IdentifierDeclStatement -> IdentifierDecl ->[2] AssignmentExpr -> CallExpression -> Callee
                    callees = Pipeline.v(d.getId()).children().has("type", "IdentifierDecl").children().has("childNum", "2").children().children().has("type", "Callee").to_list();
                   }
                  else if(((String)(d.getProperty("type"))).equals("ExpressionStatement"))
                   {
                   // ExpressionStatement -> AssignmentExpr -> CallExpression -> Callee
                    callees = Pipeline.v(d.getId()).children().has("type", "AssignmentExpr").children().has("type", "CallExpression").children().has("type", "Callee").to_list();
                   }
    
                  for(Node c : callees)
                   {
                   String func_name = (String)(c.getProperty("code"));
                     if(User_controlled_sources.arg_is_user_controlled(func_name, new Long(-1)))
                      {
                      List<Long> new_callstack = new ArrayList<>(call_stack);
                       new_callstack.add(c.getId());
                      Def_tree ret = new Def_tree(c.getId(), "", cur, false, new_callstack);
                       cur.add_sub_tree(ret);

                      Pair<List<Long>, List<Object>> dummy_pair = retrace_source_and_var_names(ret);
                      List<Long> ucs_source = dummy_pair.first;
                      List<Object> ucs_var_names = dummy_pair.second;
                       System.out.println("FOUND:");
                       System.out.println("ucs_source: " + ucs_source.toString());
                       System.out.println("ucs_var_names: " + ucs_var_names.toString());
                       ergo_set.add(new Ergo_item(task_id, ucs_source, ucs_var_names));
                       continue;
                      }
                     else
                      {
                      Long func_id = Joern_db.get_function_id_by_name(func_name);
                       System.out.println("check retval of function " + func_id.toString() + " <- " + func_name);
                      List<Long> returns = Function_sets_parameter.retval_of_func(joern_db, func_id);
                        for(Long r : returns)
                         {
                         List<Long> new_callstack = new ArrayList<>(call_stack);
                          new_callstack.add(c.getId());
                         Def_tree ret = new Def_tree(r, "", cur, true, new_callstack);
                          cur.add_sub_tree(ret);
                          q.add(ret);
                         }
                      }
                   }
                 continue;
                }
             Arg_in_call_return_type a = var_name_is_argument_in_call(joern_db, d.getId(), var_name);
//                is_arg, func_name, arg_index, nof_args = 
              System.out.println("is_arg: " + a.is_arg.toString());
               if(a.is_arg)
                {
                 System.out.println("func_name: " + a.func_name);
                 System.out.println("arg_index: " + a.arg_index.toString());
                 System.out.println("nof_args: " + a.nof_args.toString());
                  if(sets_param_dict.containsKey(a.func_name) && sets_param_dict.get(a.func_name).containsKey(a.nof_args) && sets_param_dict.get(a.func_name).get(a.nof_args).get(a.arg_index.intValue()) == 1)
                   {
                     if(1 == Data_transfer.data_transfer_sets_arg(a.func_name, a.arg_index))
                      {
                       System.out.println("Can use data_transfer");
                            // is set as tainted_arg => use data-trans
                      List<Long> stems_from = Data_transfer.get_data_transfer_for_argument(a.func_name, a.arg_index);
                        for(Long stem : stems_from)
                         {
                         Long that_args_id = get_id_for_ith_arg(joern_db, d.getId(), stem);
                           if(that_args_id == null)
                            {
                             continue;
                            }
                         List<String> in_vars = get_arg_variables(joern_db, that_args_id);
                           for(String v : in_vars)
                            {
                            Def_tree tmp = new Def_tree(that_args_id, v, q_def, false, call_stack);
                             q.add(tmp);
                            }
                         }
                       continue;
                      }
                     else
                      {
                      // is_arg => use-inter-func-stuff
                      List<Param_data_source> ds = Function_sets_parameter.get_param_data_sources(joern_db, a.func_name, a.arg_index, new ArrayList<String>(), sets_param_dict, func_ptr_candidates, func_ptr_names);
                       System.out.println("ds: " + ds.toString());
                        for(Param_data_source it : ds)
                         {
                         Def_tree par = new Def_tree(d.getId(), var_name, q_def, false, call_stack);
                         List<Long> new_callstack = new ArrayList<>(call_stack);
                          new_callstack.add(d.getId());
						 Def_tree tmp = new Def_tree(it.node_id, it.var_name, par, it.continue_at_self, new_callstack);
                          q.add(tmp);
                         }
                       continue;
                      }
                   }
                  else
                   {
                    System.out.println("function-call does not set this parameter => continue with sources for this argument");
                   Long that_args_id = get_id_for_ith_arg(joern_db, d.getId(), a.arg_index);
                   List<String> in_vars = get_arg_variables(joern_db, that_args_id);
                     for(int i=0, i_end=in_vars.size(); i<i_end; ++i)
                      {
                       in_vars.set(i, member_to_var_name(Function_sets_parameter.split_into_elements(in_vars.get(i))));
                      }
                    System.out.println("in_vars: " + in_vars.toString());
    
                   List<Neo4j2Edge> in_edges = Pipeline.v(d.getId()).inE("REACHES").toList();
                   List<String> var_elements = Function_sets_parameter.split_into_elements(member_to_var_name(var_name));
                    System.out.println("var_elements: " + var_elements.toString());
                     for(Neo4j2Edge e : in_edges)
                      {
                      String e_var = member_to_var_name(Function_sets_parameter.split_into_elements((String)(e.getProperty("var"))));;
                        if(!in_vars.contains(e_var))
                         {
                          continue;
                         }
                      List<String> e_var_split = Function_sets_parameter.split_into_elements((String)(e.getProperty("var")));
                       System.out.println("in in_vars: " + e.getId().toString() + " " + e_var_split.toString());
                      // var_name has to be prefix of e_var
                        if(!Function_sets_parameter.starts_with(e_var_split, var_elements))
                         {
                          continue;
                         }
                       System.out.println("is prefix: " + e.getId().toString() + " " + e_var_split.toString() + " " + var_elements.toString());
//                       print e.start_node
                       Neo4j2Vertex start_node = (Neo4j2Vertex)e.getProperty("start_node");
                       Def_tree v_cur = new Def_tree((Long)start_node.getId(), var_elements, q_def, true, call_stack);
                        q.add(v_cur);
                       }
                    }
                 }
    
//                is_param, func_name, param_index = is_parameter(joern_db, d._id, var_name)
              Is_parameter_return_type r = is_parameter(joern_db, d.getId(), var_name);
                if(r.is_param)
                 {
                  System.out.println("is_param: " + r.is_param.toString());
                  System.out.println("func_name: " + r.func_name);
                  System.out.println("param_index: " + r.param_index.toString());
                  System.out.println("d_type: " + (String)(d.getProperty("type")));
                 List<Long> call_site_arguments = null;
                 List<Node> tmp = null;
                   if(call_stack.size() > 0)
                    {
                    Long from_stack = call_stack.get(call_stack.size()-1);
                     System.out.println("limiting call-sites to " + from_stack.toString());
                     call_stack.remove(call_stack.size()-1);
                    String t = (String)(((Node)(Pipeline.v(from_stack).to_list().get(0))).getProperty("type"));
                      if(t.equals("Callee"))
                       {
                        tmp = Pipeline.v(from_stack).parents().ithArguments(r.param_index).to_list();
                       }
                      else if(t.equals("ExpressionStatement"))
                       {
                        tmp = Pipeline.v(from_stack).children().ithArguments(r.param_index).to_list();
                       }
                      else if(t.equals("CallExpression"))
                       {
                        tmp = Pipeline.v(from_stack).ithArguments(r.param_index).to_list();
                       }
                      else if(t.equals("Condition"))
                       {
                        tmp = Pipeline.v(from_stack).children().has("type", "CallExpression").ithArguments(r.param_index).to_list();
                       }
                      else
                       {
                        throw new Exception("expected a known type: " + t + "\t" + from_stack.toString());
                       }

                      for(Node it : tmp)
                       {
                        call_site_arguments.add(it.getId());
                       }
    
    //                    // sanity-check:
    //                    call_site_name = joern_db.runGremlinQuery("g.v(%s).code" % (from_stack))
    //                    call_site_name = call_site_name.split(" ")[0]
    //                    if(call_site_name != func_name):
    //                        raise Exception("Callstack-name-mismatch: " + call_site_name + " found, but " + func_name + " expected; check func-ptr?")
                    }
                   else
                    {
                     call_site_arguments = new ArrayList<>();
                    List<Node> tmp1 = null;
                    List<Node> tmp2 = Joern_db.getCallsTo(r.func_name);
                      for(Node t : tmp2)
                       {
                        tmp1 = Pipeline.v(t.getId()).ithArguments(r.param_index).to_list();
                         for(Node it : tmp1)
                          {
                           call_site_arguments.add(it.getId());
                          }
                       }
                      if(func_ptr_reverse_candidates.containsKey(r.func_name))
                       {
                         for(Long callee_id : func_ptr_reverse_candidates.get(r.func_name))
                          {
                          List<Node> from_func_ptr_rev = Pipeline.v(callee_id).parents().ithArguments(r.param_index).to_list();
                            for(Node it : from_func_ptr_rev)
                             {
                              System.out.println("from_func_ptr_reverse: " + new Long(it.getId()).toString());
                              call_site_arguments.add(it.getId());
                             }
                          }
                       }
                      if(func_ptr_names.containsKey(r.func_name))                                // SHORTCUT?
                       {                                                                         // SHORTCUT?
                         for(Long callee_id : func_ptr_names.get(r.func_name))                   // SHORTCUT?
                          {                                                                      // SHORTCUT?
                            for(String candidate : func_ptr_candidates.get(callee_id))           // SHORTCUT?
                             {                                                                   // SHORTCUT?
                             List<Long> func_ids = Joern_db.get_function_ids_by_name(candidate); // SHORTCUT?
                             List<Node> tmp3 = Joern_db.getCallsTo(candidate);                   // SHORTCUT?
							 List<Long> from_func_ptr = new ArrayList<>();                       // SHORTCUT?
                               for(Node n : tmp3)                                                // SHORTCUT?
                                {                                                                // SHORTCUT?
                                List<Node> args = Pipeline.v(n.getId()).ithArguments(r.param_index).to_list();        // SHORTCUT?
                                  for(Node arg : args)                                           // SHORTCUT?
                                   {                                                             // SHORTCUT?
                                    from_func_ptr.add(arg.getId());                              // SHORTCUT?
                                   }                                                             // SHORTCUT?
                                }                                                                // SHORTCUT?
                              System.out.println("from_func_ptr: " + from_func_ptr.toString());  // SHORTCUT?
                               for(Long it : from_func_ptr)                                      // SHORTCUT?
                                {                                                                // SHORTCUT?
                                 call_site_arguments.add(it);                                    // SHORTCUT?
                                }                                                                // SHORTCUT?
                             }                                                                   // SHORTCUT?
                          }                                                                      // SHORTCUT?
                       }                                                                         // SHORTCUT?
                    }
                  System.out.println("call_site_arguments: " + call_site_arguments.toString());
                 Def_tree par = new Def_tree(d.getId(), var_name, q_def, false, call_stack);
                   for(Long that_args_id : call_site_arguments)
                    {
                    List<Node> tmp1 = Pipeline.v(that_args_id).uses().to_list();
                    List<String> var_names = new ArrayList<>();
                      for(Node n : tmp1)
                       {
                        var_names.add((String)n.getProperty("code"));
                       }
                     System.out.println("var_names: " + var_names.toString());
                      for(String v : var_names)
                       {
                       Def_tree tmp_d = new Def_tree(that_args_id, v, par, false, call_stack);
                        q.add(tmp_d);
                       }
                    }
                  continue;
                 }
    
               System.out.println("not arg, lhs, or param... continue with REACHES");
              List<Node> in_defs = get_reached_by(joern_db, d.getId(), var_name);
                for(Node in_d : in_defs)
                 {
                 Def_tree tmp = new Def_tree(in_d.getId(), var_name, q_def, true, call_stack);
                  q.add(tmp);
                 }
             }
         }
        catch(Exception e)
         {
          for(StackTraceElement ste : new Throwable().getStackTrace())
           {
            System.out.println(ste);
           }
//            raise e
//            top = traceback.extract_stack()[-1]
//                print ', '.join([type(e).__name__, os.path.basename(top[0]), str(top[1])])
//            q.append(q_def)
         }
     }
   } //EOF function get_defs_of



// Return: bool is_arg, string func_name, int arg_index, int nof_args
  public static Arg_in_call_return_type var_name_is_argument_in_call(Joern_db joern_db, Long node_id, Object var_name) throws Exception
   {
   List<Node> callees = Pipeline.v(node_id).children().has("type", "CallExpression").callToCallee().to_list();

    if(callees.size() == 0)
     {
      return new Arg_in_call_return_type(false, "", -1, -1);
     }

    if(callees.size() != 1)
     {
      throw new Exception("expected one callee");
     }
   Node callee = callees.get(0);
//    print "var_name_is_argument_in_call, callee:", callee

   List<Node> arg_users = Pipeline.v(node_id).children().has("type", "CallExpression").callToArguments().to_list();
//    print "arg_users:", arg_users
   int nof_args = arg_users.size();
     for(int i=0, i_end = nof_args; i<i_end; ++i)
      {
      String arg_u_code = " " + (String)(arg_users.get(i).getProperty("code")) + " ";
//        print "arg_u_code:", arg_u_code
        if(arg_u_code.contains(" " + member_to_var_name(var_name) + " "))
         {
          return new Arg_in_call_return_type(true, (String)(callee.getProperty("code")), i, nof_args);
         }
      }
    return new Arg_in_call_return_type(false, "", -1, -1);
   }



// Return: bool is_param, string func_name, int param_index
  public static Is_parameter_return_type is_parameter(Joern_db joern_db, Long node_id, Object var_name)
   {
   Node node = (Node)(Pipeline.v(node_id).to_list().get(0));
     if(!((String)(node.getProperty("type"))).equals("Parameter"))
      {
       return new Is_parameter_return_type(false, "", -1);
      }

   String v = member_to_var_name(var_name);
//    print "v:", v
//    c = split_into_elements()
//    print "c:", c
     if(!((String)(node.getProperty("code"))).contains(v))
      {
       return new Is_parameter_return_type(false, "", -1);
      }

   Long param_index = (Long)(node.getProperty("childNum"));

   List<Node> function_def = Pipeline.v(node_id).parents().parents().to_list();
   String func_name = (String)(function_def.get(0).getProperty("code"));
    func_name = func_name.substring(0, func_name.indexOf('(')).trim();

    return new Is_parameter_return_type(true, func_name, param_index.intValue());
   }



// given the call_id and the argument-index, return the id of that argument
  public static Long get_id_for_ith_arg(Joern_db joern_db, Long node_id, Long ith_argument) throws Exception
   {
   List<Node> callees = Pipeline.v(node_id).children().has("type", "CallExpression").callToCallee().to_list();
     if(callees.size() == 0)
      {
//        return False, "", -1 // Most likely a bug
       return null;
      }

    if(callees.size() != 1)
     {
      throw new Exception("expected one callee");
     }
   Node callee = callees.get(0);
//   List<Node> arg = joern_db.runGremlinQuery("g.v(%s).children().filter{it.type == 'CallExpression'}.callToArguments().id" % (node_id)) // Most likely a bug
   List<Node> arg = Pipeline.v(callee.getId()).children().has("type", "CallExpression").callToArguments().to_list();
     if(arg.size() > ith_argument)
      {
       return arg.get(ith_argument.intValue()).getId();
      }
    return null;
   }


// Get the variables used by a certain argument of a call
  public static List<String> get_arg_variables(Joern_db joern_db, Long arg_id)
   {
   List<Node> var_nodes = Pipeline.v(arg_id).uses().to_list();
   List<String> var_names = new ArrayList<>();
      for(Node v : var_nodes)
       {
        var_names.add((String)(v.getProperty("code")));
       }
    return var_names;
   }





  public static Pair<List<Long>, List<Object>> retrace_source_and_var_names(Def_tree leaf_def_tree)
   {
   List<Pair<Long, Object>> path = leaf_def_tree.path_to_root();
   List<Long> source = new ArrayList<>();
   List<Object> var_names = new ArrayList<>();
     for(Pair<Long, Object> it : path)
      {
       source.add(it.first);
       var_names.add(it.second);
      }
    var_names.remove(var_names.size()-1);

    return new Pair<List<Long>, List<Object>>(source, var_names);
   }





// Find all sinks for a specific sink_name
// NOTE: get_calls_to does NOT include func-ptrs
  public static List<Node> find_sensitive_sinks(Joern_db joern_db, String sink_name)
   {
   List<Node> sink_calls = Joern_db.get_calls_to(sink_name);
    return sink_calls;
   }


// Get sensitive sink names from sensitive_sink-module
// Return all such sinks.
  public static List<Node> find_all_sensitive_sinks(Joern_db joern_db)
   {
   List<Node> ss = new ArrayList<>();
   HashMap<String, List<Long>> sink_names = Sensitive_sinks.get_sensitive_sinks();
     for(String sink_name : sink_names.keySet())
      {
      List<Node> for_one_sink = find_sensitive_sinks(joern_db, sink_name);
        for(Node n : for_one_sink)
         {
          ss.add(n);
         }
      }
    return ss;
   }



// NOTE: get_calls_to does NOT include func-ptrs
  public static List<Long> find_all_user_controlled_sources(Joern_db joern_db)
   {
   List<Long> ucs = new ArrayList();
   HashMap<String, List<Long>> sources = User_controlled_sources.get_user_controlled();
     for(String func_name : sources.keySet())
      {
        if(sources.get(func_name).contains(new Long(-1)))
         {
         // return parameter is user_controlled => each call is a source
         List<Node> source_calls = Joern_db.get_calls_to(func_name);
           for(Node s : source_calls)
             {
              ucs.add(s.getId());
             }
         }
        else
         {
         List<Node> source_calls = Joern_db.get_calls_to(func_name);
           for(Node s : source_calls)
            {
            // CallExpression -> ArgumentList -> [Arguments]

            List<Node> arguments = Pipeline.v(s.getId()).children().has("type", "ArgumentList").children().to_list();
            int nof_children = arguments.size();
              for(Long child_index : sources.get(func_name))
               {
                 if(nof_children > child_index)
                  {
                   ucs.add(arguments.get(child_index.intValue()).getId());
                  }
               }
            }
         }
      }
    return ucs;
   }





  public static void print_path(Joern_db joern_db, List<Long> path, List<String> var_names)
   {
     for(int i=0, i_end=path.size(); i<i_end; ++i)
      {
      Long p = path.get(i);
      Node node = (Node)(Pipeline.v(p).to_list().get(0));
        if(node.hasProperty("code"))
         {
          System.out.println(p.toString() + "\t" + (String)(node.getProperty("code")));
         }
        else
         {
          System.out.println(p.toString());
         }
        if(var_names != null && i < path.size()-1)
         {
          System.out.println("\t" + var_names.get(i));
         }
      }
   }

  public static void print_path(Joern_db joern_db, List<Long> path)
   {
    print_path(joern_db, path, null);
   }


  public static void main(String[] argv) throws Exception
   {
     if(argv.length != 0)
      {
       System.out.println("[/] Usage: no parameters");
       System.exit(1);
      }

//    test_get_ith_path();
//    System.exit(1);

   Joern_db joern_db = new Joern_db();
    joern_db.initialize();

//    print get_function_ids_by_name(joern_db, "png_crc_error")
//    print get_function_ids_by_name(joern_db, "png_process_IDAT_data")

//    print find_all_paths(joern_db, 29829, 29860, 1, "CFG_EDGE")
//    print find_all_paths_in_range(joern_db, 29829, 29860, 1, 2, "CFG_EDGE")
//
//    sys.exit(1)


//    test_get_location_tuple(joern_db)


//    arg_id = get_id_for_ith_arg(83, 0) // single_func
//    print get_arg_variables(arg_id)
//    arg_id = get_id_for_ith_arg(83, 1) // single_func
//    print get_arg_variables(arg_id)
//
//    print get_file_path_from_id(61) // single_func
//    print get_extent_of_compound(46) // single_func
//    ergo = get_if_extent(61) // if condition in single_func
//    file_path, data_dict = get_if_extent(53) // if condition for len in single_func_single_if
//    instrument_if(53)
   List<Long> ucs = find_all_user_controlled_sources(joern_db);
    System.out.println("Found " + (new Long(ucs.size())).toString() + " user controlled sources");
//    print "ucs:", ucs


   List<Long> source = null;
   List<Object> var_names = null;

   List<Node> ss = find_all_sensitive_sinks(joern_db);
    System.out.println("Found " + (new Long(ss.size())).toString() + " sensitive sinks");
//    Collections.shuffle(ss);

   HashSet<Pair<Long, Long>> already_started = new HashSet<>();
//    already_started = set([(122862, 2), (97275, 2), (42151, 2), (136196, 2), (136529, 2), (37924, 2), (63837, 2), (97882, 2), (32675, 2), (151692, 0), (29445, 2), (170768, 2), (95479, 2), (136761, 2), (171722, 0), (122544, 2), (97730, 2), (86911, 2), (23172, 2), (77137, 2), (77407, 2), (77022, 2), (32563, 2), (136610, 2), (162212, 0), (32595, 2), (76614, 2), (95243, 2), (122520, 2), (29376, 2), (22899, 0), (97528, 2), (95388, 2), (96644, 2), (123033, 2), (103293, 2), (172042, 0), (98400, 2), (124390, 2), (171209, 2), (33378, 2), (151734, 0), (23070, 0),             (136222, 2), (96430, 2), (95788, 2), (85713, 2), (96377, 2), (86889, 2), (38329, 2), (141167, 2), (162188, 0), (172064, 0), (93898, 2), (175234, 0), (97990, 2), (170996, 1), (123093, 2), (122886, 2), (103634, 2), (171759, 0), (29551, 2), (151280, 2), (97169, 2), (124326, 2), (162170, 0) ])


   LinkedList<Retrace_arg_till_source> work_queue = new LinkedList<>();
   Set<Ergo_item> ergo_set = Collections.synchronizedSet(new HashSet<Ergo_item>());
   Set<Had_already> had_already = Collections.synchronizedSet(new TreeSet<Had_already>()); // PORT should this be shared?
     for(Node s : ss)
      {
      String func = (String)(s.getProperty("code"));
       func = func.substring(0, func.indexOf(" "));
      List<Long> params = Sensitive_sinks.get_sensitive_sinks().get(func);

        for(Long p : params)
         {
         String task_id = func + "_" + p.toString() + "_" + new Long(s.getId()).toString();
          System.out.println("task_id: " + task_id);

//            if(func != "png_crc_read" or p != 2 or s._id != 81591):: // TEST just for first-real-world-path-experiments
//            if(func == "fread" and p == 2 and s._id == 47220): // TEST just for first-real-world-full-path-experiment
//           if(!func.equals("fread") || p != 2 || s.getId() != 47220) // TEST just for first-real-world-full-path-experiment
//            {
//             continue;
//            }
           if(already_started.contains(new Pair<Long, Long>(s.getId(), p)))
            {
             System.out.println("already_started");
             continue;
            }

//          print_path(joern_db, s._id])
         Retrace_arg_till_source entry = new Retrace_arg_till_source(joern_db, task_id, ergo_set, had_already, s.getId(), new Long(p));
          work_queue.add(entry);
         }
      }

    System.out.println("len(work_queue): " + (new Long(work_queue.size())).toString());

//   Timecap_queue timecap_queue = new Timecap_queue(work_queue, 1, 60*60);
   Timecap_queue timecap_queue = new Timecap_queue(work_queue, 1, 60);
    timecap_queue.start();

     for(Ergo_item e : ergo_set)
      {
       System.out.println(e.toString());
      }
    System.out.println("number of UCS-to-SS paths: " + (new Long(ergo_set.size())).toString());



    // Get the call graph
   HashMap<Long, HashSet<Long>> func_id_call_graph = Get_call_graph.get_func_id_call_graph(joern_db);

   HashMap<Long, Set<String>> func_ptr_candidates = (HashMap<Long, Set<String>>)Pickle.load_from_file("func_ptr_candidates.ser");
//   HashMap<Long, List<String>> func_ptr_names = (HashMap<String, List<Long>>)Pickle.load_from_file("func_ptr_names.ser");
    
    // augment call graph to include function pointers
     for(Long func_ptr_callee : func_ptr_candidates.keySet())
      {
      // get it's function
      Long function_id = (Long)(((Node)(Pipeline.v(func_ptr_callee).to_list().get(0))).getProperty("functionId"));

      // collect the func-ids of the functions it may call
      List<Long>  called_function_ids = new ArrayList<>();
        for(String func_name : func_ptr_candidates.get(func_ptr_callee))
         {
           for(Long func_id : Joern_db.get_function_ids_by_name(func_name))
            {
             called_function_ids.add(func_id);
            }
         }

      // add them to the call-graph
        if(!func_id_call_graph.containsKey(function_id))
         {
          func_id_call_graph.put(function_id, new HashSet<Long>());
         }
        for(Long c : called_function_ids)
         {
          func_id_call_graph.get(function_id).add(c);
         }
      }

   PrintWriter f = new PrintWriter("func_id_call_graph.txt", "UTF-8");
     for(Long from_id : func_id_call_graph.keySet())
      {
       f.print(from_id.toString());
       f.print(": ");
      ArrayList<Long> tmp = new ArrayList<>(func_id_call_graph.get(from_id));
       Collections.sort(tmp);
       f.print(tmp.toString());
       f.print("\n");
      }
    f.close();


     for(Ergo_item e : ergo_set)
      {
      List<Long> the_source = e.source;
      List<Object> var_names_org = e.var_names;

        try
         {
         Collections.reverse(the_source);
         Collections.reverse(var_names_org);
         List<String> the_var_names = new ArrayList<>();
           for(Object v : var_names_org)
            {
             the_var_names.add(member_to_var_name(v));
            }
        
          System.out.println("the_source: " + the_source.toString());
          System.out.println("the_var_names: " + the_var_names.toString());

         List<Long> cfg_parents = Find_control_flow_paths.find_cfg_parents(joern_db, the_source);
//          print_path(joern_db, cfg_parents, the_var_names);
        
            // Remove double elements, both in reached_parents and in var_names
         List<Long> indices = Find_control_flow_paths.find_double_element_indices(cfg_parents);
          cfg_parents = Find_control_flow_paths.remove_indices(cfg_parents, indices);
          the_var_names = Find_control_flow_paths.remove_indices(the_var_names, indices);
        
          System.out.println("cfg_parents (after removing double elements):");
          System.out.println("cfg_parents: " + cfg_parents.toString());
          System.out.println("the_var_names: " + the_var_names.toString());
          print_path(joern_db, cfg_parents, the_var_names);

         List<List<List<Long>>> path_chain = Find_control_flow_paths.find_all_cfg_connections(joern_db, cfg_parents, func_id_call_graph);
         Long nof_paths = Find_control_flow_paths.count_paths(path_chain);
          System.out.println("nof_paths: " + nof_paths.toString());
        
         Long path_elements = new Long(0);
         Long sum_path_element_length = new Long(0);
         Long max_path_element_length = new Long(0);
           for(List<List<Long>> paths : path_chain)
            {
              for(List<Long> p : paths)
               {
                path_elements += 1;
                sum_path_element_length += p.size();
                max_path_element_length = Math.max(max_path_element_length, p.size());
               }
            }
         Float avg_path_element_length = new Float(((float)sum_path_element_length) / path_elements);
          System.out.println("avg_path_element_length: " + avg_path_element_length.toString());
          System.out.println("max_path_element_length: " + max_path_element_length.toString());
        
         List<Long> cur_path = Find_control_flow_paths.get_ith_path(path_chain, 0);
          System.out.println("cur_path.size(): " + (new Long(cur_path.size())).toString());
          print_path(joern_db, cur_path);
        
         List<String> overarch = Find_control_flow_paths.find_var_name_overarch(cur_path, cfg_parents, the_var_names);
          System.out.println("overarch: " + overarch.toString());
          assert overarch.size() == cur_path.size() - 1;


// PORT instrumentation: find_all_checks, is_relevant_check
        
        
//         List<Long> all_checks = find_all_checks(joern_db, cur_path);
//          System.out.println("all_checks: " + all_checks.toString());
//        
//           for(Long c : all_checks)
//            {
//            String overarched_by = overarch.get(cur_path.indexOf(c)-1);
//        //        print c, "is overarched by", overarched_by
//            Boolean is_rel = is_relevant_check(joern_db, c, overarched_by);
//        
//              if(is_rel)
//               {
//               String its_code = (String)(Pipeline.v(c).to_list().get(0).getProperty("code"));
//                System.out.println(c.toString() + " seems to be relevant check: " + its_code);
////                instrument_if(60);
//               }
//            }
        }
       catch(Exception ex)
        {
          for(StackTraceElement ste : new Throwable().getStackTrace())
           {
            System.out.println(ste);
           }
        }
      }
   } // EOF main

 } // EOF class
