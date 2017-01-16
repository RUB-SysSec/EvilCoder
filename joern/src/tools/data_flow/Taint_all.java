package tools.data_flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.PrintWriter;

import org.neo4j.graphdb.Node;

class Taint_all
 {
  public static HashMap<String, Object> data;
  public static HashMap<String, HashMap<Long, List<Long>>> libc_sets_param_dict;
  public static HashMap<Long, HashSet<String> > func_ptr_candidates = null;
  static
   {
    data = (HashMap<String, Object>)Pickle.load_from_file("data_trans.ser");
    libc_sets_param_dict = (HashMap<String, HashMap<Long, List<Long>>>)data.get("sets_param");
   }


  public static void fill_func_ptr_candidates(Joern_db joern_db) throws Exception
   {
     if(func_ptr_candidates != null) return;
    System.out.println("Collecting func_ptr_candidates");
    func_ptr_candidates = new HashMap<>();

    func_ptr_candidates = Find_all_function_pointers.get_func_ptr_candidates(joern_db);     //SHORTCUT
   }


  private static Long set_max(Set<Long> s)
   {
   Long max_elem = s.iterator().next();
     for(Long it : s)
      {
        if(it > max_elem)
         {
          max_elem = it;
         }
      }
    return max_elem;
   }

  public static void main(String[] argv) throws Exception
   {
    // TODO: passing sets_param_dict accomplishes nothing,
    // because to_taint does not seem to get filled in that case.

     if(argv.length > 2)
      {
       System.out.println("Usage: [full_call_graph_file] [sets_param_dict_file]");
       System.exit(1);
      }

   Joern_db joern_db = new Joern_db();
    joern_db.initialize();
//    func_id = get_function_id_by_name(joern_db, "png_set_packswap")
//    print function_sets_parameter_i(joern_db, func_id, 0)
//    sys.exit(1)

    fill_func_ptr_candidates(joern_db);
   HashMap<String, List<Long>> func_ptr_names = new HashMap<>();

     for(Long callee_id : func_ptr_candidates.keySet())             //SHORTCUT
      {                                                             //SHORTCUT
      Node n = (Node)(Pipeline.v(callee_id).to_list().get(0));      //SHORTCUT
      String code = (String)(n.getProperty("code"));                //SHORTCUT
        if(!func_ptr_names.containsKey(code))                       //SHORTCUT
         {                                                          //SHORTCUT
          func_ptr_names.put(code, new ArrayList<Long>());          //SHORTCUT
         }                                                          //SHORTCUT
       func_ptr_names.get(code).add(callee_id);                     //SHORTCUT
      }                                                             //SHORTCUT
    Pickle.save_to_file("func_ptr_candidates.ser", func_ptr_candidates);
    Pickle.save_to_file("func_ptr_names.ser", func_ptr_names);


   HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>> full_cg = null;
     if(argv.length < 1)
      {
       System.out.println("Computing call-graph");
       full_cg = Get_call_graph.get_call_graph(joern_db);
       Pickle.save_to_file("full_cg.ser", full_cg);
      }
     else
      {
       full_cg = (HashMap<Pair<String, Long>, Pair<String, HashSet<Pair<String, Long>>>>)Pickle.load_from_file(argv[1]);
      }

   HashMap<String, HashMap<Long, List<Long>>> sets_param_dict = new HashMap<>(libc_sets_param_dict);
     if(argv.length > 1)
      {
       sets_param_dict = (HashMap<String, HashMap<Long, List<Long>>>)Pickle.load_from_file(argv[2]);
      }


   HashMap<Pair<String, Long>, HashSet<Pair<String, Long>>> CG = Get_call_graph.get_simple_call_graph(full_cg);

   Set<Pair<String, Long>> all_funcs = new HashSet<>();
     for(Pair<String, Long> k : CG.keySet())
      {
       all_funcs.add(k);
        for(Pair<String, Long> it : CG.get(k))
         {
          all_funcs.add(it);
         }
      }
   Long all_funcs_counter = new Long(all_funcs.size());


    // use topo to get sequence of funcs
   Best_effort_topological_sort tp = new Best_effort_topological_sort(CG);
    // for each func...
   Long counter = new Long(0);
   List<Pair<String, Long>> to_taint = new ArrayList<>();
     for(Iterator<Pair<String, Long>> iter = tp.iterator(); iter.hasNext();)
      {
      Pair<String, Long> func_info = iter.next();

       counter += 1;
      String func_name = func_info.first;
      Long nof_params = func_info.second;
       System.out.println("Handling function " + counter.toString() + " of " + all_funcs_counter.toString() + ": " + func_name);
       System.out.flush();

      List<Long> sets_param = new ArrayList<>();
        if(sets_param_dict.containsKey(func_name) && sets_param_dict.get(func_name).containsKey(nof_params))
         {
           for(Long param : sets_param_dict.get(func_name).get(nof_params))
            {
              if(param != 1)
               {
                continue;
               }
             System.out.println("tainting param " + param.toString() + " for func " + func_name + ", because sets_param_dict says so");
             to_taint.add(new Pair<String, Long>(func_name, param));
            }
         }
        else if(Data_transfer.data_transfer_has(func_name))
         {
         List<Pair<Long, List<Long>>> dt = Data_transfer.get_data_transfer(func_name);
           if(dt.size() == 0)
            {
             continue;
            }
         HashMap<Long, Long> tmp_dic = new HashMap<>();
           for(Pair<Long, List<Long>> it : dt)
            {
            Long param = it.first;
             tmp_dic.put(param, new Long(1));
             System.out.println("tainting param " + param.toString() + " for func " + func_name + ", because data_transfer says so");
             to_taint.add(new Pair<String, Long>(func_name, param));
            }

         Long m = Math.max(set_max(tmp_dic.keySet())+1, nof_params);
          sets_param = new ArrayList<>();
           for(int i=0; i<m; ++i) sets_param.add(new Long(-1));
           for(Long it : tmp_dic.keySet())
            {
             sets_param.set(it.intValue(), new Long(1));
            }
         }
        else
         {
          sets_param = new ArrayList<>();
           for(int i=0; i<nof_params; ++i)
            {
             sets_param.add(new Long(0));
            }
           for(Long i=new Long(0); i<nof_params; ++i)
            {
            // find out if ith param is defining
            List<Long> ns = Joern_db.get_function_ids_by_name(func_name);
              if(ns.size() == 0)
               {
                System.out.println("Cannot find definition for function " + func_name);
                 if(!func_ptr_names.containsKey(func_name))
                  {
                   System.out.println("also not a func-ptr => next try");
                   continue;
                  }
                 for(Long callee_id : func_ptr_names.get(func_name))
                  {
                    for(String candidate : func_ptr_candidates.get(callee_id))
                     {
                     // if in func-ptr-candidates, add those functions to ns...
                     List<Long> func_ids = Joern_db.get_function_ids_by_name(candidate);
                       for(Long func_id : func_ids)
                        {
                         ns.add(func_id);
                        }
                     }
                  }
               }
              if(ns.size() == 0)
               {
                System.out.println("not even func-ptr did give candidates");
                continue;
               }
            HashSet<Long> guesses = new HashSet<>();
              for(Long n : ns)
               {
//                    print "func_name:", func_name
//                    print "n:", n
               Long sets_it = Function_sets_parameter.function_sets_parameter_i(joern_db, n, i, sets_param_dict);
//                    print "sets_it:", sets_it
                guesses.add(sets_it);
               }
              if(guesses.size() == 1)
               {
                sets_param.set(i.intValue(), guesses.iterator().next());
               }
              else
               {
                sets_param.set(i.intValue(), set_max(guesses)); // 0
               }

              if(sets_param.get(i.intValue()) == 1)
               {
                System.out.println("tainting param " + i.toString() + " for func " + func_name + ", because it sets this param");
                to_taint.add(new Pair<String, Long>(func_name, i));
               }
            }
         }

        if(!sets_param_dict.containsKey(func_name))
         {
          sets_param_dict.put(func_name, new HashMap<Long, List<Long>>());
         }
        if(!sets_param_dict.get(func_name).containsKey(nof_params))
         {
          System.out.println("func_name, sets_param: " + func_name + " " + sets_param.toString());
          sets_param_dict.get(func_name).put(nof_params, sets_param);
         }
      }

     for(String it : sets_param_dict.keySet())
      {
        if(libc_sets_param_dict.containsKey(it))
         {
          continue;
         }

        for(Long it2 : sets_param_dict.get(it).keySet())
         {
          System.out.println(it + " -> " + sets_param_dict.get(it).get(it2).toString());
         }
      }

    Pickle.save_to_file("sets_param_dict.ser", sets_param_dict);

   List<Pair<String, Long>> to_taint_unique = new ArrayList<>();
   Set<Pair<String, Long>> had = new HashSet<>();
     for(Pair<String, Long> func_param : to_taint)
      {
        if(had.contains(func_param))
         {
          System.out.println("Avoided tainting " + func_param.toString() + " twice");
         }
        else
         {
          had.add(func_param);
          to_taint_unique.add(func_param);
         }
      }

   PrintWriter f = new PrintWriter("generated_taint_all.txt", "UTF-8");
     for(Pair<String, Long> func_param : to_taint_unique)
      {
       f.print(func_param.first + "\t" + func_param.second.toString() + "\n");
      }
    f.close();
   }
 }
