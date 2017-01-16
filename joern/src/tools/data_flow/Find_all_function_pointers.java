package tools.data_flow;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import java.nio.file.Paths;
import java.nio.file.Files;

import org.neo4j.graphdb.Node;

import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.pipes.PipeFunction;

class Find_all_function_pointers
 {
  private static HashSet<String> externally_defined_functions = null;
  public static void fill_externally_defined_functions(Joern_db joern_db)
   {
// completeType    extern void ( )
// baseType        extern void
// type            Decl
// identifier      extern_func
    if(externally_defined_functions != null) return;

    externally_defined_functions = new HashSet<>();
//    decls = joern_db.runGremlinQuery("queryNodeIndex('type:Decl').filter{it.completeType.startsWith('extern') && it.completeType.endsWith(')')}.identifier")
//    decls = joern_db.runGremlinQuery("g.V.filter{it.type == 'Decl' && it.completeType.startsWith('extern') && it.completeType.endsWith(')')}.identifier")

   List<Node> decl_nodes = Joern_db.queryNodeIndex("type:Decl");
     for(Node d : decl_nodes)
      {
      String completeType = (String)d.getProperty("completeType");
        if(completeType.startsWith("extern") && completeType.endsWith(")"))
         {
         String identifier = (String)d.getProperty("identifier");
          externally_defined_functions.add(identifier);
         }
      }
   }


  public static Boolean func_definition_exists(Joern_db joern_db, String func_name)
   {
    fill_externally_defined_functions(joern_db);
    return externally_defined_functions.contains(func_name);
   }


  private static HashSet<String> glibc_function_names = null;
  public static void fill_glibc_function_names() throws Exception
   {
     if(glibc_function_names != null) return;

   String data = new String(Files.readAllBytes(Paths.get("glibc_function_names.txt")));
   String[] lines = data.split("\n", -1);
     for(int i=0, i_end = lines.length; i<i_end; ++i)
      {
       lines[i] = lines[i].trim();
      }

   glibc_function_names = new HashSet<>();
     for(String l : lines)
      {
        if(l.equals("")) continue;
       glibc_function_names.add(l);
      }
   }
    
    
  private static ArrayList<String> assign_expression_codes = null;
  public static Boolean is_function_pointer(Joern_db joern_db, String callee_code) throws Exception
   {
    fill_glibc_function_names();

     if(glibc_function_names.contains(callee_code)) return false;

   // starts with * (
     if(callee_code.startsWith("* (")) return true;

   // uses a field
     if(callee_code.contains(" -> ") || callee_code.contains(" . ")) return true;

   // there exists no func-declaration for it
   List<Long> func_ids = Joern_db.get_function_ids_by_name(callee_code);
     if(func_ids.size() != 0) return false;

   // there exists no func-DEFINITION for it
     if(!func_definition_exists(joern_db, callee_code)) return false;

   // there an assign to it
     if(assign_expression_codes == null)
      {
      List<Node> assign_expr = Joern_db.queryNodeIndex("type:AssignmentExpr");
       assign_expression_codes = new ArrayList<>();
        for(Node n : assign_expr)
         {
          assign_expression_codes.add((String)n.getProperty("code"));
         }
      }

   boolean assigned = false;
     for(String a : assign_expression_codes)
      {
        if(a.contains(callee_code + " ="))
         {
          assigned = true;
          break;
         }
      }
     if(assigned) return true;

//    assigned = joern_db.runGremlinQuery("g.V.filter{it.type == 'AssignmentExpr'}.filter{-1 != it.code.indexOf(' %s = ')}" % callee_code)
//    print "assigned:", assigned
//    if(len(assigned) != 0):
//        return True

    return false;
   }


  public static HashMap<Long, Long> find_all_function_pointers(Joern_db joern_db) throws Exception
   {
   List<Node> callees = joern_db.queryNodeIndex("type:Callee");
   HashMap<String, HashSet<Long> > callee_codes = new HashMap<>();
    for(Node c : callees)
     {
     Long c_id = c.getId();
     String code = (String)c.getProperty("code");
        if(!callee_codes.containsKey(code))
         {
          callee_codes.put(code, new HashSet<Long>());
         }
      callee_codes.get(code).add(c_id);
     }

  HashMap<Long, Long> func_ptrs = new HashMap<>();
    for(String code : callee_codes.keySet())
     {
       if(!is_function_pointer(joern_db, code)) continue;

//        print code, "->", list(callee_codes[code])
       for(Long c_id : callee_codes.get(code))
        {
        // id -> Callee -> parents() = CallExpression -> children() ArgumentList -> children(), len
        List<Node> args = Pipeline.v(c_id).parents().children().has("type", "ArgumentList").children().to_list();
         func_ptrs.put(c_id, new Long(args.size()));
//            func_ptrs[code] = len(args)
        }
     }
//    print "len(func_ptrs)", len(func_ptrs)
    return func_ptrs;
   }



  public static HashSet<String> find_all_func_names(Joern_db joern_db) throws Exception
   {
    fill_glibc_function_names();
    fill_externally_defined_functions(joern_db);

   HashSet<String> all_funcs = new HashSet(glibc_function_names);
     for(String it : externally_defined_functions) all_funcs.add(it);

    // get all function-defs, extract their names
   List<Node> func_defs = joern_db.queryNodeIndex("type:FunctionDef");
     for(Node n : func_defs)
      {
      String code = (String)n.getProperty("code");
      String[] splitters = code.split(" ", -1);
       all_funcs.add(splitters[0]);
      }

    return all_funcs;
   }


  public static HashSet<String> all_used_func_names(Joern_db joern_db) throws Exception
   {
   HashSet<String> all_funcs = find_all_func_names(joern_db);

   HashSet<String> used = new HashSet<>();

//    uses = joern_db.runGremlinQuery("g.V.uses()")
   List<Node> uses = Joern_db.queryNodeIndex("type:Symbol");
   HashSet<Long> checked_already = new HashSet<>();
    for(Node u : uses)
     {
     Long id = (Long)u.getId();
        if(checked_already.contains(id))
         {
          continue;
         }

      checked_already.add(id);
     String code = (String)u.getProperty("code");
        if(all_funcs.contains(code))
         {
         List<Node> context_list = Pipeline.v(id).in("USE").to_list();
           if(context_list.isEmpty())
            {
             continue;
            }
         String context = (String)context_list.get(0).getProperty("code");
            if(context.contains(code + " ("))
             {
              used.add(code);
             }
         }
     }
    return used;

//    assigned = set()
//    assign_codes = joern_db.runGremlinQuery("g.V.filter{it.type == 'AssignmentExpr'}.code")
//    for a in assign_codes:
//        a = a.replace(")", "") # func-ptr may be casted
//        a = a.split(" ")[-1] # saves endswith-operations, allows "in"
//        if(a in all_funcs):
//            assigned.add(a)
//
//    return assigned
  }


  public static HashSet<Long> get_nof_arguments_of_func(Joern_db joern_db, String func_name)
   {
   HashSet<Long> nof_args = new HashSet<>();
   List<Long> func_ids = Joern_db.get_function_ids_by_name(func_name);
     for(Long f_id : func_ids)
      {
//        print "f_id:", f_id
        // FunctionDef -> ParameterList -> children -> len
      List<Node> args = Pipeline.v(f_id).functionToAST().children().has("type", "ParameterList").children().to_list();
        if(args.size() > 0)
         {
           if(args.get(args.size()-1).getProperty("code").equals("..."))
            {
              for(Long i = new Long(args.size()-1); i<=10; ++i)
               {
                nof_args.add(i);
               }
            }
           else
            {
             nof_args.add(new Long(args.size()));
            }
         }
      }

    // if there were funcs, but no args => assume length-0 parameter list
    if(nof_args.size() == 0 && func_ids.size() > 0)
     {
      nof_args.add(new Long(0));
     }

    if(nof_args.size() == 0)
     {
     // Fallback: if we cant find something, allow everything
       for(Long i = new Long(0); i<=10; ++i)
        {
         nof_args.add(i);
        }
     }
    return nof_args;
   }


  public static HashMap<Long, HashSet<String> > get_func_ptr_candidates(Joern_db joern_db) throws Exception
   {
   HashMap<Long, Long> func_ptrs = find_all_function_pointers(joern_db);
//    print "func_ptrs:", func_ptrs

   HashSet<String> used = all_used_func_names(joern_db);
   Long of = new Long(used.size());
   Long counter = new Long(0);
   HashMap<Long, HashSet<String> > funcs_using_nof_args = new HashMap<>();
    for(String u : used)
     {
      ++counter;
      System.out.println(counter.toString() + " of " + of.toString() + ": " + u);
      System.out.flush();
     HashSet<Long> nof_args = get_nof_arguments_of_func(joern_db, u);
        for(Long n : nof_args)
         {
           if(!funcs_using_nof_args.containsKey(n))
            {
             funcs_using_nof_args.put(n, new HashSet<String>());
            }
          funcs_using_nof_args.get(n).add(u);
         }
//        print u, list(nof_args)
    }
//    print funcs_using_nof_args


  HashMap<Long, HashSet<String> > func_ptr_candidates = new HashMap<>();
    for(Long callee : func_ptrs.keySet())
     {
      func_ptr_candidates.put(callee, new HashSet<String>());
       for(String candidate : funcs_using_nof_args.get(func_ptrs.get(callee)))
        {
         func_ptr_candidates.get(callee).add(candidate);
        }
     }
//    print "func_ptr_candidates:", func_ptr_candidates
    return func_ptr_candidates;
   }
            

  public static void main(String[] args) throws Exception
   {
   Joern_db joern_db = new Joern_db();
    joern_db.initialize();

    // ENSURE follow_field.c is loaded in Database
    System.out.println(get_func_ptr_candidates(joern_db).toString());
   }
}

