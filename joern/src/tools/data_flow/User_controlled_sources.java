package tools.data_flow;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

class User_controlled_sources
 {
   public static HashMap<String, List<Long>> user_controlled_funcs;
   public static HashMap<String, List<Long>> user_controlled_in_for;
   private static List<Long> list_helper(int ... args)
    {
    List<Long> ret = new ArrayList<>();
      for(int arg : args)
       {
        ret.add(new Long(arg));
       }
     return ret;
    }

   static
    {
     user_controlled_funcs = new HashMap<>();
// Key: Function name
// Value: User controlled argument (negative, if output-parameter)
     user_controlled_funcs.put("getenv", list_helper(1));
     user_controlled_funcs.put("gets", list_helper(0));
     user_controlled_funcs.put("fgets", list_helper(0));
     user_controlled_funcs.put("fread", list_helper(0));
     user_controlled_funcs.put("read", list_helper(1));
     user_controlled_funcs.put("recv", list_helper(1));
     user_controlled_funcs.put("recvfrom", list_helper(1));
     //user_controlled_funcs.put("recvmsg", list_helper(1));
     user_controlled_funcs.put("scanf", list_helper(1, 2, 3, 4, 5, 6, 7, 8, 9));
     user_controlled_funcs.put("fscanf", list_helper(2, 3, 4, 5, 6, 7, 8, 9));
     user_controlled_funcs.put("getc", list_helper(1));
     user_controlled_funcs.put("fgetc", list_helper(1));


     user_controlled_in_for = new HashMap<>();
// Key: Function name
// Value: User controlled argument (negative, if output-parameter)
     user_controlled_in_for.put("getc", list_helper(1));
     user_controlled_in_for.put("fgetc", list_helper(1));
    }

   public static HashMap<String, List<Long>> get_user_controlled()
    {
 return user_controlled_funcs;
    }

   public static List<Long> get_user_controlled_args(String func_name)
    {
 return user_controlled_funcs.get(func_name);
    }

   public static Boolean func_is_user_controlled(String func_name)
    {
     return user_controlled_funcs.containsKey(func_name);
    }

   public static Boolean arg_is_user_controlled(String func_name, Long ith_arg)
    {
      if(!user_controlled_funcs.containsKey(func_name))
       {
        return false;
       }
     return user_controlled_funcs.get(func_name).contains(ith_arg);
    }


//def get_function_name_from_ExpressionStatement(joern_db, node_id):
//    code = joern.runGremlinQuery("g.v(%s).out(IS_AST_PARENT).filter{it.type == 'CallExpression'}.out(IS_AST_PARENT).filter(it.type == 'Callee').gather{it.code}" % (node_id))
//    if(len(code) != 1):
//        raise Exception("Expected one Callee")
//    return code[0]
//
//def ExpressionStatement_is_user_controlled(joern_db, node_id):
//    func_name = get_function_name_from_ExpressionStatement(joern_db, node_id)
//    return func_is_user_controlled(func_name)
//
 }

