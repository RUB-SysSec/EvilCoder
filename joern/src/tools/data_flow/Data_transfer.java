package tools.data_flow;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

class Data_transfer
 {
// Key: Func name
// Value: List of data-transfer pairs; each pair (i, [j]): arg i stems from args j
// Values < 0: Function itself fills it... e.g. fread
 static HashMap<String, List<Pair<Long, List<Long>>>> data_transfer = null;

   static
    {
    HashMap<String, Object> data = (HashMap<String, Object>)Pickle.load_from_file("data_trans.ser");

     data_transfer = (HashMap<String, List<Pair<Long, List<Long>>>>)data.get("data_trans");

// NOTE: Irrelevant, as init_glibc_data_trans does this
// data_transfer["memcpy"] = [(0, [1])]
// data_transfer["strcpy"] = [(0, [1])]
// data_transfer["strncpy"] = [(0, [1])]
// data_transfer["sprintf"] = [(0, [2, 3, 4, 5, 6, 7, 8, 9])]
// data_transfer["snprintf"] = [(0, [3, 4, 5, 6, 7, 8, 9])]
// data_transfer["strcat"] = [(0, [0, 1])]
// data_transfer["sscanf"] = [(0, [2, 3, 4, 5, 6, 7, 8, 9])]
// data_transfer["fread"] = [(0, [-1])]
    }



   public static HashMap<String, List<Pair<Long, List<Long>>>> get_data_transfer_dict()
    {
     return data_transfer;
    }


   public static List<Long> get_data_transfer_for_argument(String func_name, Long ith_argument) throws Exception
    {
    List<Pair<Long, List<Long>>> val = get_data_transfer(func_name);
      for(Pair<Long, List<Long>> v : val)
       {
         if(v.first == ith_argument)
          {
           return v.second;
          }
       }
     throw new Exception("Argument " + ith_argument.toString() + " not found in list " + val.toString());
    }


   public static List<Pair<Long, List<Long>>> get_data_transfer(String func_name) throws Exception
    {
      if(!data_transfer.containsKey(func_name))
       {
        throw new Exception("Function not found");
       }
     return data_transfer.get(func_name);
    }


   public static Boolean data_transfer_has(String func_name)
    {
     return data_transfer.containsKey(func_name);
    }


   public static Long data_transfer_sets_arg(String func_name, Long ith_argument)
    {
      if(!data_transfer_has(func_name))
       {
        return new Long(0);
       }

      try
       {
        get_data_transfer_for_argument(func_name, ith_argument);
        return new Long(1);
       }
      catch(Exception e)
       {
        return new Long(-1);
       }
    }
 }

