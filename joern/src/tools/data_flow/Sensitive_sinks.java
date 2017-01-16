package tools.data_flow;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

class Sensitive_sinks
 {
   public static HashMap<String, List<Long>> sensitive_sinks;
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
     sensitive_sinks = new HashMap<>();

    // Key: Function name
    // Value: Dunno yet
     sensitive_sinks.put("strcpy", list_helper(1));
     sensitive_sinks.put("memcpy", list_helper(2)); // [1,2]
     //sensitive_sinks.put("png_crc_read", list_helper(1, 2)); // TEST
     sensitive_sinks.put("strncpy", list_helper(2)); //# [1,2]
     sensitive_sinks.put("strcat", list_helper(1));
     sensitive_sinks.put("sprintf", list_helper(1));
     sensitive_sinks.put("snprintf", list_helper(2));
     sensitive_sinks.put("malloc", list_helper(0));
     sensitive_sinks.put("realloc", list_helper(1));
     sensitive_sinks.put("calloc", list_helper(0, 1));
     sensitive_sinks.put("fread", list_helper(2));
    }

   public static HashMap<String, List<Long>> get_sensitive_sinks()
    {
	 return sensitive_sinks;
    }

 }
