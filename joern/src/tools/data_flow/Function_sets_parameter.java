package tools.data_flow;

import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

class Function_sets_parameter
 {
   public static Set<String> primitive_types;
   static
    {
     primitive_types = new HashSet<>();
     primitive_types.add("char");
     primitive_types.add("int");
     primitive_types.add("signed char");
     primitive_types.add("unsigned char");
     primitive_types.add("short");
     primitive_types.add("short int");
     primitive_types.add("signed short");
     primitive_types.add("signed short int");
     primitive_types.add("unsigned short");
     primitive_types.add("unsigned short int");
     primitive_types.add("int");
     primitive_types.add("signed int");
     primitive_types.add("unsigned");
     primitive_types.add("unsigned int");
     primitive_types.add("long");
     primitive_types.add("long int");
     primitive_types.add("signed long");
     primitive_types.add("signed long int");
     primitive_types.add("unsigned long");
     primitive_types.add("unsigned long int");
     primitive_types.add("long long");
     primitive_types.add("long long int");
     primitive_types.add("signed long long");
     primitive_types.add("signed long long int");
     primitive_types.add("unsigned long long");
     primitive_types.add("unsigned long long int");
     primitive_types.add("float");
     primitive_types.add("double");
     primitive_types.add("long double");
    }



   public static List<Long> retval_of_func(Joern_db joern_db, Long func_id)
    {
    List<Node> returns = Joern_db.queryNodeIndex("type:ReturnStatement AND functionId:" + func_id.toString());
//    print returns
    List<Long> candidates = new ArrayList<>();
      for(Node r : returns)
       {
       List<Node> ret_children = Pipeline.v(r.getId()).children().to_list();
        for(Node c : ret_children)
         {
          System.out.println("ret_child: " + new Long(c.getId()).toString());
          candidates.add(c.getId());
//            get_rhs_val(c)
         }
       }
     return candidates;
    }



// Checks if a symbol is among the uses of a node
   public static Boolean node_uses_symbol(Joern_db joern_db, Long node_id, Long symbol_id)
    {
    List<Node> ergo = Pipeline.v(node_id).uses().has("id", symbol_id.toString()).to_list();
     return !ergo.isEmpty();
    }


   public static Boolean node_uses_symbols(Joern_db joern_db, Long node_id, Set<Long> symbol_ids)
    {
    List<Node> ergo = Pipeline.v(node_id).uses().to_list(); //.code" % (node_id))
      for(Node e : ergo)
       {
       String its_code = (String)(e.getProperty("code"));
         if(symbol_ids.contains(its_code))
          {
           return true;
          }
       }
     return false;
    }



// Given a call_id, find the type of the ith-argument
   public static String get_parameter_type(Joern_db joern_db, Long call_id, Long ith_argument)
    {
    List<Node> t = Pipeline.v(call_id).functionToStatements().has("type", "Parameter").has("childNum", ith_argument.toString()).children().has("type", "ParameterType").to_list();
    Set<String> codes = new HashSet<>();
      for(Node n : t)
       {
        codes.add((String)(n.getProperty("code")));
       }
      if(codes.size() == 1)
       {
        return codes.iterator().next().trim();
       }
      else
       {
        return "";
       }
    }


   public static String get_function_name_from_function(Joern_db joern_db, Long node_id)
    {
    Node n = (Node)(Pipeline.v(node_id).to_list().get(0));
     return (String)(n.getProperty("name"));
    }


   public static Set<List<String>> find_aliases(Joern_db joern_db, List<Node> stmt_list, Node symbol)
    {
    List<String> symbol_codes = new ArrayList<>();
     symbol_codes.add((String)(symbol.getProperty("code")));
     return find_subfield_aliases(joern_db, stmt_list, symbol_codes);
    }



// Function, which finds out, whether a certain function-call,
// given by it's node id, modifies it's i-th parameter.
// If the function is present, it looks into that function and applies heuristics.
// For library functions, it uses the data-transfer-functionality.

//Heuristics
//    Mentioned in data_transfer    -> Forward result
//    Const                -> False
//    Primitive type            -> False
//    Assigned to            -> True
//    Used in IncDec             -> True
//    Used as tainted param        -> True        (e.g. used as parameter 0 of memcpy)
//    Only used as non-tainted param in known funcs => False
//    Else                -> Dunno

// Return-Value:
//    -1 -> Function does _NOT_ set this value
//    0  -> dunno
//    1  -> Function _DOES_ set this value
// NOTE: Correlate changes with get_param_data_sources
   public static Long function_sets_parameter_i(Joern_db joern_db, Long call_id, Long ith_param, HashMap<String, HashMap<Long, List<Long>>> gathered_so_far) throws Exception
    {
    List<String> empty = new ArrayList<>();
     return function_sets_field(joern_db, call_id, ith_param, empty, gathered_so_far);
    }

   public static Long function_sets_parameter_i(Joern_db joern_db, Long call_id, Long ith_param) throws Exception
    {
    HashMap<String, HashMap<Long, List<Long>>> empty = new HashMap<>();
     return function_sets_parameter_i(joern_db, call_id, ith_param, empty);
    }


   public static Set<Long> get_all_children(Joern_db joern_db, Long node_id)
    {
    Set<Long> children = new HashSet<>();
    LinkedList<Long> new_nodes = new LinkedList<>();
     new_nodes.add(node_id);
      while(!new_nodes.isEmpty())
       {
       Long cur = new_nodes.pollFirst();
       List<Node> its_children = Pipeline.v(cur).children().to_list();
         for(Node c : its_children)
          {
            if(children.contains(c.getId()))
             {
              continue;
             }
            new_nodes.add(c.getId());
            children.add(c.getId());
          }
       }
     return children;
    }




// Function, which collects the data-sources for a certain parameter, given by id.
// Assumption: function_sets_parameter == 1
// Assumption: no data_transfer given (but it _may_ exist)
// NOTE: Correlate changes with function_sets_field

// Cases:
//    Assigned to            -> True
//    Used as tainted param        -> True        (e.g. used as parameter 0 of memcpy)

// Return-Value:
//    [(id, var_name, continue_at_self)^+]
   public static List<Param_data_source> get_param_data_sources(Joern_db joern_db, String this_func_name, Long ith_param, List<String> subfields, HashMap<String, HashMap<Long, List<Long>>> gathered_so_far, HashMap<Long, HashSet<String>> func_ptr_candidates, HashMap<String, List<Long>> func_ptr_names) throws Exception
    {
    List<Param_data_source> data_sources = new ArrayList<>();

    Long function_id = Joern_db.get_function_id_by_name(this_func_name);
     System.out.println("function_id: " + function_id.toString());
      if(function_id == -1)
       {
         if(func_ptr_names.containsKey(this_func_name))
          {
          Long first = func_ptr_names.get(this_func_name).get(0);
          List<Param_data_source> ds = null;
            for(String func_name : func_ptr_candidates.get(first))
             {
              ds = get_param_data_sources(joern_db, func_name, ith_param, subfields, gathered_so_far, func_ptr_candidates, func_ptr_names);
               for(Param_data_source it : ds)
                {
                 data_sources.add(it);
                }
             }
           return data_sources;
          }
         else
          {
           return data_sources;
          }
       }

    // Find the symbol of the parameter
    Node symbol = (Node)(Pipeline.v(function_id).functionToStatements().has("type", "Parameter").has("childNum", ith_param.toString()).defines().to_list().get(0));

    List<Node> stmt_list = Pipeline.v(function_id).functionToStatements().to_list();

    // Find aliases
    List<String> with_subfields = new ArrayList<>(subfields);
     with_subfields.add(0, (String)(symbol.getProperty("code")));
    Set<List<String>> aliases = find_subfield_aliases(joern_db, stmt_list, with_subfields);
     System.out.println("aliases: " + aliases.toString());

      for(Node s : stmt_list)
       {
//        print "s._id:", s._id
       Boolean uses_one = false;
         for(List<String> a : aliases)
          {
//            print "a[-1]:", a[-1]
//            if(node_uses_symbols(joern_db, s._id, [a[-1]])): // For some reason, this does not work
            if(((String)(s.getProperty("code"))).contains(a.get(a.size()-1)))
             {
              uses_one = true;
              break;
             }
          }
         if(!uses_one)
          {
           continue;
          }
        System.out.println("uses one: " + new Long(s.getId()).toString());


       List<Node> callees = Pipeline.v(s.getId()).has("type", "CallExpression").callToCallee().to_list();
       List<Long> callee_ids = new ArrayList<>();
         for(Node c : callees)
          {
           callee_ids.add(c.getId());
          }

        // Heuristic: Assigned -> True
        // Check if the symbol is on the lhs of this statement (if it is an Assignment-statement)
//        ergo = joern_db.runGremlinQuery("g.v(%s).children().filter{it.type == 'AssignmentExpr'}.lval().uses().filter{it.id == %s}" % (s._id, symbol._id))
       List<Node> assignment = null;
         if(((String)(s.getProperty("type"))).equals("IdentifierDeclStatement"))
          {
// use: & s2_alias -> s2_2
             // IdentifierDeclStatement -> IdentifierDecl -> AssigmentExpression
           assignment = Pipeline.v(s.getId()).children().has("childNum", "0").children().has("type", "AssignmentExpr").to_list();
          }
         else if(((String)(s.getProperty("type"))).equals("ExpressionStatement"))
          {
           assignment = Pipeline.v(s.getId()).children().has("type", "AssignmentExpr").to_list();
          }
 
         if(assignment != null && assignment.size() == 1)
          {
          Long assignment_id = assignment.get(0).getId();
          System.out.println("assignment_id: " + assignment_id.toString());
 
          String lval_identifier = null;
          List<Node> lvals = Pipeline.v(assignment_id).lval().to_list();
           System.out.println("lvals: " + lvals.toString());
            if(lvals.size() == 1)
             {
             Node lval = lvals.get(0);
              System.out.println("HERE");
               if(((String)(lval.getProperty("type"))).equals("Identifier"))
                {
                 lval_identifier = (String)(lval.getProperty("code"));
                }
               else if(((String)(lval.getProperty("type"))).equals("PtrMemberAccess") || ((String)(lval.getProperty("type"))).equals("MemberAccess"))
                {
                 lval_identifier = (String)(lval.getProperty("code")); // s1_alias -> s1_2
                }
             }
            else
             {
              System.out.println("fallback");
             // should not happen. Fallback to code-parsing :/
             String assignment_code = (String)(assignment.get(0).getProperty("code"));
              System.out.println("assignment_code: " + assignment_code);
              lval_identifier = assignment_code.split("=", -1)[0].trim();
               if(lval_identifier.startsWith("* "))
                {
                 lval_identifier = lval_identifier.substring(2);
                }
             }
           System.out.println("lval_identifier: " + lval_identifier);
 
            if(lval_identifier != null)
             {
             List<String> elements = split_into_elements(lval_identifier);
    //            if(node_uses_symbols(joern_db, lval[0], aliases)):
//                if(elements in aliases):
               if(in_aliases(elements, aliases))
                {
                List<Object> rhs_uses = new ArrayList<>();
                Node rval = (Node)(Pipeline.v(assignment_id).rval().to_list().get(0));
                 System.out.println("rval: " + rval.toString());
                  if(((String)(rval.getProperty("type"))).equals("Identifier"))
                   {
                    rhs_uses.add((String)(rval.getProperty("code")));
                   }
                  else if(((String)(rval.getProperty("type"))).equals("PtrMemberAccess") || ((String)(rval.getProperty("type"))).equals("MemberAccess"))
                   {
                   String code = (String)(rval.getProperty("code")); // s1_alias -> s1_2
                   List<String> tmp_elements = split_into_elements(code);
                    rhs_uses.add(tmp_elements);
                   }
                  else
                   {
                   Set<Long> all_children_ids = get_all_children(joern_db, rval.getId());
                     for(Long a : all_children_ids)
                      {
                        if(callee_ids.contains(a))
                         {
                         String func_name = (String)(((Node)(Pipeline.v(a).to_list().get(0))).getProperty("code"));
                         Long func_id = Joern_db.get_function_id_by_name(func_name);
                         System.out.println("check retval of function " + func_id.toString() + " <- " + func_name);
                         List<Long> returns = retval_of_func(joern_db, func_id);
                           for(Long it : returns)
                            {
// PORT: this is a fuckup. retval_of_func returns Longs... theres something wrong
                             rhs_uses.add(it);
                            }
                         }
                      }
                   //TODO gets all the uses... those in callees, too
                   List<Node> uses = Pipeline.v(rval.getId()).uses().to_list();
                     for(Node use : uses)
                      {
                      String u = (String)(use.getProperty("code"));
//                            print "u:", u
                       u = u.replaceAll("& ", "");
                      List<String> tmp_elements = split_into_elements(u);
                       rhs_uses.add(tmp_elements);
                      }
                   }
         
                  for(Object rhs_use : rhs_uses)
                   {
                    data_sources.add(new Param_data_source(s.getId(), rhs_use, true));
                   }
                }
             }
          }
 
         //Heuristic: Used as tainted param -> yes
 
         // Walk over callees of this statement
//        callees = joern_db.runGremlinQuery("g.v(%s).match{it.type == 'CallExpression'}.callToCallee()" % (s._id))
         for(Node callee : callees)
          {
          String func_name = (String)(callee.getProperty("code"));
           System.out.println("callee_func_name: " + func_name);
            if(Data_transfer.data_transfer_has(func_name))
             {
              System.out.println("found in data-trans");
             // Walk over the arguments the known-lib-func taints
               for(Pair<Long, List<Long>> it : Data_transfer.get_data_transfer(func_name))
                {
                Long tainted_arg = it.first;
     
                // Find out if the tainted_arg uses the symbol
//                    ergo = joern_db.runGremlinQuery("g.v(%s).calleeToCall().ithArguments('%s').uses().filter{it.id == %s}" % (callee._id, tainted_arg, symbol._id))
                List<Node> ergo = Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).uses().to_list();
                 System.out.println("ergo: " + ergo.toString());
                  for(Node e : ergo)
                   {
                   List<String> elements = split_into_elements((String)(e.getProperty("code")));
                    System.out.println("elements: " + elements.toString());
//                        if(elements in aliases):
                     if(in_aliases(elements, aliases))
                      {
                       System.out.println("e " + (new Long(callee.getId())).toString() + " arg " + tainted_arg.toString() + " uses that param");
                      List<Long> tainted_arg_sources = it.second;
                       System.out.println("follow its sources: " + tainted_arg_sources.toString());
                        for(Long t : tainted_arg_sources)
                         {
                           if(t == -1)
                            {
                             System.out.println("special sign for internally-set argument");
                            Long arg_id = ((Node)(Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).to_list().get(0))).getId();
                             System.out.println("arg_id: " + arg_id.toString());
 
                             data_sources.add(new Param_data_source(arg_id, elements, true));
                            }
                           else
                            {
//                                    raise Exception("Not Tested")
                            Long arg_id = ((Node)(Pipeline.v(callee.getId()).calleeToCall().ithArguments(t).to_list().get(0))).getId();
                             System.out.println("arg_id: " + arg_id.toString());
                            List<Node> var_names = Pipeline.v(arg_id).uses().to_list();
                              for(Node v : var_names)
                               {
                               String var_name = (String)(v.getProperty("code"));
                                System.out.println("var_name: " + var_name);
                                data_sources.add(new Param_data_source(arg_id, split_into_elements(var_name), true));
                               }
                            }
                         }
                      }
                   }
                }
             }
            else if(gathered_so_far.containsKey(func_name))
             {
              System.out.println("func_name: " + func_name);
             List<Node> argument_nodes = Pipeline.v(callee.getId()).calleeToCall().callToArguments().to_list();
             List<Long> arguments = new ArrayList<>();
               for(Node n : argument_nodes)
                {
                 arguments.add(n.getId());
                }
             Long nof_arguments = new Long(arguments.size());
               if(!gathered_so_far.get(func_name).containsKey(nof_arguments))
                {
                 continue;
                }
             List<Long> sets_params = gathered_so_far.get(func_name).get(nof_arguments);
              System.out.println("sets_params: " + sets_params.toString());
             // Walk over the arguments of this previously seen func
               for(int i=0, i_end = sets_params.size(); i<i_end; ++i)
                {
                  if(sets_params.get(i) != 1)
                   {
                    continue;
                   }
                 System.out.println((new Long(callee.getId())).toString() + symbol);
                Long tainted_arg = new Long(i);
                // Find out if the tainted_arg uses the symbol
//                    ergo = joern_db.runGremlinQuery("g.v(%s).calleeToCall().ithArguments('%s').uses().filter{it.id == %s}" % (callee._id, tainted_arg, symbol._id))
                List<Node> ergo = Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).uses().to_list();
                  for(Node e : ergo)
                   {
                   List<String> elements = split_into_elements((String)(e.getProperty("code")));
//                        if(elements in aliases):
                     if(in_aliases(elements, aliases))
                      {
                       System.out.println("need to follow arg: " + tainted_arg.toString());
                      Long arg_id = ((Node)(Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).to_list().get(0))).getId();
                       System.out.println("arg_id: " + arg_id.toString());
                       data_sources.add(new Param_data_source(arg_id, elements, true));
                      }
                   }
                }
             }
          }
       }

     return data_sources;
    }



   public static List<String> split_into_elements(String code)
    {
    List<String> ptr_elements = Arrays.asList(code.split(" -> ", -1));
    List<String> elements = new ArrayList<>();
      for(String it : ptr_elements)
       {
       List<String> splitters = Arrays.asList(it.split(" . ", -1));
         for(String splitter : splitters)
          {
           elements.add(splitter.trim());
          }
       }
//    print "elements:", elements
     return elements;
    }


   public static Set<List<String>> find_subfield_aliases(Joern_db joern_db, List<Node> stmt_list, List<String> symbols)
    {
// TODO check alias startswith *, for primitive types... non-struct... (dammit typedef)

    // get aliases for all symbols[:-1]
    Set<List<String>> pre_aliases = new HashSet<>();
      if(symbols.size() > 1)
       {
       List<String> without_last = new ArrayList<>(symbols);
        without_last.remove(without_last.size()-1);
        pre_aliases = find_subfield_aliases(joern_db, stmt_list, without_last);
       }

    String last_symbol = symbols.get(symbols.size()-1);
    if(pre_aliases.isEmpty())
     {
     List<String> last_symbol_list = new ArrayList<>();
      last_symbol_list.add(last_symbol);
      pre_aliases.add(last_symbol_list);
     }
    else
     {
     Set<List<String>> new_pre_aliases = new HashSet<>();
       for(List<String> p : pre_aliases)
        {
         p.add(last_symbol);
         new_pre_aliases.add(p);
        }
      pre_aliases = new_pre_aliases;
     }

    // find aliases for <pre_aliases> -> last_symbol
    Set<String> self_aliases = new HashSet<>();

    for(Node s : stmt_list)
     {
//        print
     List<Node> assignment = null;
       if(((String)(s.getProperty("type"))).equals("IdentifierDeclStatement"))
        {
// use: & s2_alias -> s2_2
        // IdentifierDeclStatement -> IdentifierDecl -> AssigmentExpression
         assignment = Pipeline.v(s.getId()).children().has("childNum", "0").children().has("type", "AssignmentExpr").to_list();
        }
       else if(((String)(s.getProperty("type"))).equals("ExpressionStatement"))
        {
         assignment = Pipeline.v(s.getId()).children().has("type", "AssignmentExpr").to_list();
        }

       if(assignment == null)
        {
         continue;
        }

       if(assignment.size() != 1)
        {
         continue;
        }
     Long assignment_id = assignment.get(0).getId();
//        print "assignment_id:", assignment_id
     String lval_identifier = null;
     List<Node> lval = Pipeline.v(assignment_id).lval().to_list(); //[0]
//        print "lval:", lval
       if(lval.size() == 1)
        {
         lval = Pipeline.v(lval.get(0).getId()).has("type", "Identifier").to_list(); //.code" % (lval._id))
          if(lval.size() != 1)
           {
            continue;
           }
         lval_identifier = (String)(lval.get(0).getProperty("code"));
//            print lval_identifier
        }
       else
        {
        // should not happen. Fallback to code-parsing :/
        String assignment_code = (String)(((Node)(Pipeline.v(assignment_id).to_list().get(0))).getProperty("code"));
//            print "assignment_code:", assignment_code
         lval_identifier = assignment_code.split("=", -1)[0].trim();
          if(lval_identifier.startsWith("* "))
           {
            lval_identifier = lval_identifier.substring(2);
           }
//        print "lval_identifier:", lval_identifier
        }

      Node rval = (Node)(Pipeline.v(assignment_id).rval().to_list().get(0));
//        print "rval:", rval
        if(((String)(rval.getProperty("type"))).equals("Identifier"))
         {
           if(((String)(rval.getProperty("type"))).equals(last_symbol)) // param
            {
              if(!self_aliases.contains(lval_identifier))
               {
                self_aliases.add(lval_identifier);
               }
            }
         }
        else if(((String)(rval.getProperty("type"))).equals("PtrMemberAccess") || ((String)(rval.getProperty("type"))).equals("MemberAccess"))
         {
         String code = (String)(rval.getProperty("code")); // s1_alias -> s1_2
         List<String> elements = split_into_elements(code);
    
           if(pre_aliases.contains(elements))
            {
            // found an alias... take left side
              if(!self_aliases.contains(lval_identifier))
               {
                self_aliases.add(lval_identifier);
               }
             break;
            }
         }
        else
         {
         List<Node> uses = Pipeline.v(assignment_id).uses().to_list();
           for(Node use : uses)
            {
            String u = (String)(use.getProperty("code"));
//                print "u:", u
             u = u.replaceAll("& ", "");
            List<String> elements = split_into_elements(u);
              if(pre_aliases.contains(elements))
               {
               // found an alias... take left side
                 if(!self_aliases.contains(lval_identifier))
                  {
                   self_aliases.add(lval_identifier);
                  }
                break;
               }
            }
         }
      }



    Set<List<String>> aliases = new HashSet<>(pre_aliases);
//    print "self_aliases:", self_aliases
      for(String a : self_aliases)
       {
       List<String> tmp = new ArrayList<>();
        tmp.add(a);
        aliases.add(tmp);
       }
     return aliases;
    }

//struct s1
// {
//  int s1_1;
//  s2 *s1_2;
// };
//
//struct s2
// {
//  int s2_1;
//  int s2_2;
// }
//
// s1 *s1_alias = param;
// => s1_alias is alias for param
// => s1_alias->s1_1 is alias for param->s1_1
// => s1_alias->s1_2 is alias for param->s1_2
// => s1_alias->s1_2->s2_1 is alias for param->s1_2->s2_1
// => s1_alias->s1_2->s2_2 is alias for param->s1_2->s2_2
//
// s2 *s2_alias = s1_alias->s1_2;
// => s2_alias is alias for param->s1_2;
// => s2_alias->s2_1 is alias for param->s1_2->s2_1
// => s2_alias->s2_2 is alias for param->s1_2->s2_2
   private static List<String> list_helper(String ... args)
    {
    List<String> ret = new ArrayList<>();
      for(String it : args)
       {
        ret.add(it);
       }
     return ret;
    }


   public static void test_find_subfield_aliases(Joern_db joern_db) throws Exception
    {
    Long func_id = Joern_db.get_function_id_by_name("create_aliases");
    List<Node> stmt_list = Pipeline.v(func_id).functionToStatements().to_list();
     System.out.println(stmt_list.get(0));

    Set<List<String>> ret = null;

     ret = find_subfield_aliases(joern_db, stmt_list, list_helper("param"));
     System.out.println("ret: " + ret.toString());
     assert ret.contains(list_helper("param"));
     assert ret.contains(list_helper("s1_alias"));

     ret = find_subfield_aliases(joern_db, stmt_list, list_helper("param", "s1_1"));
     System.out.println("ret: " + ret.toString());
     assert ret.contains(list_helper("param", "s1_1"));
     assert ret.contains(list_helper("s1_alias", "s1_1"));

     ret = find_subfield_aliases(joern_db, stmt_list, list_helper("param", "s1_2"));
     System.out.println("ret: " + ret.toString());
     assert ret.contains(list_helper("param", "s1_2"));
     assert ret.contains(list_helper("s1_alias", "s1_2"));
     assert ret.contains(list_helper("s2_alias"));

     ret = find_subfield_aliases(joern_db, stmt_list, list_helper("param", "s1_2", "s2_1"));
     System.out.println("ret: " + ret.toString());
     assert ret.contains(list_helper("param", "s1_2", "s2_1"));
     assert ret.contains(list_helper("s1_alias", "s1_2", "s2_1"));
     assert ret.contains(list_helper("s2_alias", "s2_1"));

     ret = find_subfield_aliases(joern_db, stmt_list, list_helper("param", "s1_2", "s2_2"));
     System.out.println("ret: " + ret.toString());
     assert ret.contains(list_helper("param", "s1_2", "s2_2"));
     assert ret.contains(list_helper("s1_alias", "s1_2", "s2_2"));
     assert ret.contains(list_helper("s2_alias", "s2_2"));
     assert ret.contains(list_helper("s2_2_alias"));
    }



   public static void test_function_sets_field(Joern_db joern_db) throws Exception
    {
    Long func_id = Joern_db.get_function_id_by_name("create_aliases");
    List<Node> stmt_list = Pipeline.v(func_id).functionToStatements().to_list();

    Long ret = null;
     ret = function_sets_field(joern_db, func_id, new Long(0), list_helper("s1_1"));
     System.out.println("ret: " + ret.toString());
     assert ret == 1;

     ret = function_sets_field(joern_db, func_id, new Long(0), list_helper("s1_2"));
     System.out.println("ret: " + ret.toString());
     assert ret == -1;

     ret = function_sets_field(joern_db, func_id, new Long(0), list_helper("s1_2", "s2_1"));
     System.out.println("ret: " + ret.toString());
     assert ret == 1;

     ret = function_sets_field(joern_db, func_id, new Long(0), list_helper("s1_2", "s2_2"));
     System.out.println("ret: " + ret.toString());
     assert ret == 1;
    }




   public static Boolean starts_with(List<String> list_full, List<String> list_maybe_start)
    {
      if(list_full.size() < list_maybe_start.size())
       {
        return false;
       }
      for(int i=0, i_end=list_maybe_start.size(); i<i_end; ++i)
       {
         if(!list_full.get(i).equals(list_maybe_start.get(i)))
          {
           return false;
          }
       }
     return true;
    }

   public static Boolean in_aliases(List<String> elements, Set<List<String>> aliases)
    {
      for(List<String> a : aliases)
       {
         if(starts_with(elements, a))
          {
           return true;
          }
       }
     return false;
    }


// Parameters:
//    call_id        in this function
//    ith_param     the ith_param
//    subfields    list of field-names
//    gathered_so_far    for caching-purposes

// Return-Value:
//    -1 -> Function does _NOT_ set this value
//    0  -> dunno
//    1  -> Function _DOES_ set this value
   public static Long function_sets_field(Joern_db joern_db, Long call_id, Long ith_param, List<String> subfields) throws Exception
    {
    HashMap<String, HashMap<Long, List<Long>>> empty = new HashMap<>();
     return function_sets_field(joern_db, call_id, ith_param, subfields, empty);
    }

   public static Long function_sets_field(Joern_db joern_db, Long call_id, Long ith_param, List<String> subfields, HashMap<String, HashMap<Long, List<Long>>> gathered_so_far) throws Exception
    {
    String this_func_name = get_function_name_from_function(joern_db, call_id);

      // Heuristic: Mentioned in data_transfer or in gathered_so_far -> Forward result
      if(subfields.isEmpty())
       {
       Long dt_ret = Data_transfer.data_transfer_sets_arg(this_func_name, ith_param);
         if(dt_ret != 0)
          {
           return dt_ret;
          }
      
         if(gathered_so_far.containsKey(this_func_name))
          {
            for(Long it : gathered_so_far.get(this_func_name).keySet())
             {
               if(it > ith_param)
                {
                 return gathered_so_far.get(this_func_name).get(it.intValue()).get(ith_param.intValue());
  //                    return gathered_so_far[this_func_name][it]
                }
             }
          }
       }

    // if function has less than i arguments...
    List<Node> args = Pipeline.v(call_id).functionToStatements().has("type", "Parameter").to_list();
      if(args.size() <= ith_param)
       {
        return new Long(-1);
       }


    // Heuristic: Const -> False
    String param_type = get_parameter_type(joern_db, call_id, ith_param);
      if(param_type.startsWith("const"))
       {
        return new Long(-1);
       }


    // Heuristic: Primitive Type -> False
      if(primitive_types.contains(param_type))
       {
        return new Long(-1);
       }


    // Find the symbol of the parameter
    Node symbol = (Node)(Pipeline.v(call_id).functionToStatements().has("type", "Parameter").has("childNum", ith_param.toString()).defines().to_list().get(0));

    List<Node> stmt_list = Pipeline.v(call_id).functionToStatements().to_list();

    // Find aliases
    List<String> with_subfields = new ArrayList<>();
     with_subfields.add((String)(symbol.getProperty("code")));
      for(String subfield : subfields)
       {
        with_subfields.add(subfield);
       }
    Set<List<String>> aliases = find_subfield_aliases(joern_db, stmt_list, with_subfields);
//    print "aliases:", aliases


    //Heuristic: Only used as non-tainted param in known funcs => False
    Boolean used_in_unknown_funcs = false;

      for(Node s : stmt_list)
       {
//        print "s._id:", s._id
//        print "s.code:", s["code"]
       Boolean uses_one = false;
         for(List<String> a : aliases)
          {
//            print "a[-1]:", a[-1]
//            if(node_uses_symbols(joern_db, s._id, [a[-1]])): // For some reason, this does not work
            if(((String)(s.getProperty("code"))).contains(a.get(a.size()-1)))
             {
              uses_one = true;
              break;
             }
          }
        if(!uses_one)
         {
          continue;
         }
//        print "uses one:", s._id

        // Heuristic: Assigned -> True
        // Check if the symbol is on the lhs of this statement (if it is an Assignment-statement)
      List<Node> lval = Pipeline.v(s.getId()).children().has("type", "AssignmentExpr").lval().to_list();
        if(!lval.isEmpty())
         {
//            print "lval.code:", lval[0]["code"]
         List<String> elements = split_into_elements((String)(lval.get(0).getProperty("code")));
//            if(node_uses_symbols(joern_db, lval[0], aliases)):
           if(in_aliases(elements, aliases))
            {
             return new Long(1);
            }
         }


        // Heuristic: Used in IncDec -> True
        // Check if the symbol is in an IncDec)
      List<Node> op = Pipeline.v(s.getId()).children().has("type", "UnaryOp").children().has("type", "IncDecOp").to_list();
        if(!op.isEmpty())
         {
         String its_code = (String)(op.get(0).getProperty("code"));
          System.out.println("opcode: " + its_code);
         List<String> elements = split_into_elements(its_code);
//            if(node_uses_symbols(joern_db, op[0], aliases)):
            if(in_aliases(elements, aliases))
             {
              return new Long(1);
             }
          }


        // Walk over callees of this statement
      List<Node> callees = Pipeline.v(s.getId()).has("type", "CallExpression").callToCallee().to_list();
        for(Node callee : callees)
         {
         String func_name = (String)(callee.getProperty("code"));
           if(Data_transfer.data_transfer_has(func_name))
            {
            // Walk over the arguments the known-lib-func taints
               for(Pair<Long, List<Long>> it : Data_transfer.get_data_transfer(func_name))
                {
                Long tainted_arg = it.first;
                    // Find out if the tainted_arg uses the symbol
//                    ith_arg = joern_db.runGremlinQuery("g.v(%s).calleeToCall().ithArguments('%s').id" % (callee._id, tainted_arg))[0]
//                    if(node_uses_symbols(joern_db, ith_arg, aliases)):
                List<Node> ith_arg = Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).to_list();
                  if(ith_arg.size() != 1)
                   {
                    continue;
                   }
                List<String> elements = split_into_elements((String)(ith_arg.get(0).getProperty("code")));
                  if(in_aliases(elements, aliases))
                   {
                    return new Long(1);
                   }
                }
             }
            else if(gathered_so_far.containsKey(func_name))
             {
             List<Node> arguments = Pipeline.v(callee.getId()).calleeToCall().callToArguments().to_list(); //.id" % (callee._id))
             Long nof_arguments = new Long(arguments.size());
              if(!gathered_so_far.get(func_name).containsKey(nof_arguments))
               {
                continue;
               }

             List<Long> sets_params = gathered_so_far.get(func_name).get(nof_arguments);
             // Walk over the arguments of this previously seen func
               for(int i=0, i_end=sets_params.size(); i<i_end; ++i)
                {
                  if(sets_params.get(i) != 1)
                   {
                    continue;
                   }
                Long tainted_arg = new Long(i);
                // Find out if the tainted_arg uses the symbol
//                    ith_arg = joern_db.runGremlinQuery("g.v(%s).calleeToCall().ithArguments('%s').id" % (callee._id, tainted_arg))[0]
//                    if(node_uses_symbols(joern_db, ith_arg, aliases)):
                Node ith_arg = (Node)(Pipeline.v(callee.getId()).calleeToCall().ithArguments(tainted_arg).to_list().get(0));
                String ith_arg_code = (String)(ith_arg.getProperty("code"));
                List<String> elements = split_into_elements(ith_arg_code);
                  if(in_aliases(elements, aliases))
                   {
                    return new Long(1);
                   }
                }
             }
            else
             {
              used_in_unknown_funcs = true;
             }
          }
       }

      if(!used_in_unknown_funcs)
       {
        return new Long(-1);
       }

    // No heuristic applies... dunno
     return new Long(0);
    }






   public static void self_test() throws Exception
    {
     // ENSURE follow_field.c is loaded in Database
    Joern_db joern_db = new Joern_db();
     joern_db.initialize();
 
     test_find_subfield_aliases(joern_db);
     test_function_sets_field(joern_db);
    }
 }
