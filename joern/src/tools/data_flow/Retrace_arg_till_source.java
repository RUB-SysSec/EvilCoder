package tools.data_flow;
import java.util.concurrent.Callable;

import java.util.Set;
import java.util.List;
import org.neo4j.graphdb.Node;



class Retrace_arg_till_source implements Callable<Void>
 {
 private Joern_db joern_db;
 public String task_id;
 private Set<Ergo_item> ergo_set;
 private Set<Had_already> had_already;
 private Long call_id;
 private Long ith_argument;

// Retrace the i-th arg of a certain call to it's source.
// Returns the source and whether that source is user-controlled.
   public Retrace_arg_till_source(Joern_db joern_db, String task_id, Set<Ergo_item> ergo_set, Set<Had_already> had_already, Long call_id, Long ith_argument)
    {
     this.joern_db = joern_db;
     this.task_id = task_id;
     this.ergo_set = ergo_set;
     this.had_already = had_already;
     this.call_id = call_id;
     this.ith_argument = ith_argument;
    }

    @Override
   public Void call() throws Exception
    {
     System.out.println("STARTING ON " + task_id + ": " + call_id.toString() + ", " + ith_argument.toString());
 
    Node arg = null;
      while(true)
       {
         try
          {
           arg = Find_data_paths.get_argument_i(joern_db, call_id, ith_argument);
           break;
          }
         catch(Exception e)
          {
           ;
          }
       }
//    print "arg:", arg
    List<String> var_names = Find_data_paths.get_arg_variables(joern_db, arg.getId());
//    print "var_names:", var_names
      for(String v : var_names)
       {
        Find_data_paths.get_defs_of(joern_db, arg.getId(), v, task_id, ergo_set, had_already);
       }
 
     return null;
    }
 }


