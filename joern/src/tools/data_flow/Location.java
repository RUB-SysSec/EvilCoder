package tools.data_flow;

import org.neo4j.graphdb.Node;
import java.util.HashMap;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

class Location
 {
  public String file_name;
  public String function_name;
  public Long line_no;
  public String extent;

   public Location(String file_name, String function_name, Long line_no, String extent)
    {
     this.file_name = file_name;
     this.function_name = function_name;
     this.line_no = line_no;
     this.extent = extent;
    }

   public Location(String file_name, String function_name, int line)
    {
     this.file_name = file_name;
     this.function_name = function_name;
     this.line_no = new Long(line);
     this.extent = null;
    }

   @Override
   public int hashCode()
    {
    int hash = 1;
     hash = 31 * hash + file_name.hashCode();
     hash = 31 * hash + function_name.hashCode();
     hash = 31 * hash + line_no.hashCode();
     return hash;
    }
 
   @Override
   public boolean equals(Object obj)
    {
      if(!(obj instanceof Location)) return false;
      if(this == obj) return true;
    Location other = (Location)obj;
      if(!file_name.equals(other.file_name)) return false;
      if(!function_name.equals(other.function_name)) return false;
      if(!line_no.equals(other.line_no)) return false;
// Left out extent on purpose
     return true;
    }


   @Override
   public String toString()
    {
    String s = "";
     s += file_name;
     s += ":";
     s += function_name;
     s += "@";
     s += line_no;
     return s;
    }

   public static String get_file_path(Joern_db joern_db, Node node) throws Exception
    {
    List<Node> nodes = Pipeline.v((Long)(node.getProperty("functionId"))).in().has("type", "File").to_list(); //.filepath""" % node['functionId']
      if(nodes.size() == 0)
       {
        throw new Exception("No file path found");
       }
    String file_path = (String)(nodes.get(0).getProperty("filepath"));
     return file_path;
    }


   public static String get_file_path_from_id(Joern_db joern_db, Long node_id) throws Exception
    {
    List<Node> nodes = Pipeline.v(node_id).to_list();
     return get_file_path(joern_db, nodes.get(0));
    }



   public static Long get_general_parent(Joern_db joern_db, Long node_id) throws Exception
    {
//AST_EDGE = 'IS_AST_PARENT'
//CFG_EDGE = 'FLOWS_TO'
//USES_EDGE = 'USE'
//DEFINES_EDGE = 'DEF'
//DATA_FLOW_EDGE = 'REACHES'
//FUNCTION_TO_AST_EDGE = 'IS_FUNCTION_OF_AST'
//CFG_TO_FUNCION_EDGE = 'IS_FUNCTION_OF_CFG' // JANNIK
    List<Node> parent;

     parent = Pipeline.v(node_id).in("IS_AST_PARENT").to_list();
      if(!parent.isEmpty())
       {
//        print "ast_parent:", parent
        return parent.get(0).getId();
}

     parent = Pipeline.v(node_id).in("IS_FUNCTION_OF_AST").to_list();
      if(!parent.isEmpty())
       {
//        print "func_ast_parents:", parent
        return parent.get(0).getId();
       }

     parent = Pipeline.v(node_id).in("IS_FUNCTION_OF_CFG").to_list();
      if(!parent.isEmpty())
       {
//        print "func_cfg_parents:", parent
        return parent.get(0).getId();
       }
     throw new Exception("Cannot find general parent for " + node_id.toString());
    }

   public static String get_function_name_for_node_id(Joern_db joern_db, Long node_id) throws Exception
    {
    Long cur_id = node_id;
    Long func_id = null;
      while(true)
       {
        func_id = (Long)(((Node)(Pipeline.v(cur_id).to_list().get(0))).getProperty("functionId"));
         if(func_id != null)
          {
           break;
          }
        cur_id = get_general_parent(joern_db, cur_id);
//        print "cur_id:", cur_id
       }
    String func_name = (String)(((Node)(Pipeline.v(func_id).to_list().get(0))).getProperty("name"));
     return func_name;
    }


   public static String get_location_for_node_id(Joern_db joern_db, Long node_id) throws Exception
    {
    Long cur_id = node_id;
    String location = null;
      while(true)
       {
       List<Node> nodes = Pipeline.v(cur_id).to_list();
       Node first = nodes.get(0);
         if(first.hasProperty("location"))
          {
           location = (String)(first.getProperty("location"));
           break;
          }
        cur_id = get_general_parent(joern_db, cur_id);
//        print "cur_id:", cur_id
       }
     return location;
    }

   public static HashMap<String, String> read_files = null;
   static
    {
     read_files = new HashMap<>();
    }

   public static Location get_location_tuple(Joern_db joern_db, Long node_id) throws Exception
    {
    String the_type = (String)(((Node)(Pipeline.v(node_id).to_list().get(0))).getProperty("type"));
      if(the_type.equals("CFGExitNode") || the_type.equals("Symbol"))
       {
        throw new Exception("Cannot find location for CFGExitNode or Symbol");
       }
    String file_name = get_file_path_from_id(joern_db, node_id);
//System.out.println("file_name: " + file_name);
    String func_name = get_function_name_for_node_id(joern_db, node_id);
//System.out.println("func_name: " + func_name);
    String location = get_location_for_node_id(joern_db, node_id);
//System.out.println("location: " + location);

    String[] loc_splitters = location.split(":", -1);
    Long line_no = Long.parseLong(loc_splitters[0]);

      if(!read_files.containsKey(file_name))
       {
       byte[] encoded = Files.readAllBytes(Paths.get(file_name));
       String data = new String(encoded, StandardCharsets.UTF_8);
        read_files.put(file_name, data);
       }
//    extent = read_files[file_name][line_no-1]
    String extent = get_source_range(read_files.get(file_name), Long.parseLong(loc_splitters[2]), Long.parseLong(loc_splitters[3]));
     return new Location(file_name, func_name, line_no, extent);
    }

// stolen from instrumentation.py
   public static String get_source_range(String data, Long start, Long end)
    {
	 return data.substring(start.intValue(), 1 + end.intValue());
    }



   public static void test_get_location_tuple(Joern_db joern_db) throws Exception
    {
    // Testing get_location_tuple for libpng, with function "png_do_write_transformations"
    List<Node> all_nodes_of_func = Pipeline.Vs().has("functionId", "130007").to_list();
      for(Node a : all_nodes_of_func)
       {
       String its_type = (String)(a.getProperty("type"));
         if(its_type.equals("CFGExitNode") || its_type.equals("Symbol"))
          {
           continue;
          }
        System.out.println(a.getId());
        System.out.println(get_location_tuple(joern_db, new Long(a.getId())).toString());
       }
    }


 }

