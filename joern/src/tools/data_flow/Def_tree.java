package tools.data_flow;

import java.util.List;
import java.util.ArrayList;

class Def_tree
 {
   public Long node_id;
   public Object var_name;
   public Def_tree parent;
   public Boolean continue_at_self;
   public List<Long> call_stack;
   public List<Def_tree> sub_trees;

   public Def_tree(Long node_id, Object var_name, Def_tree parent, Boolean continue_at_self, List<Long> call_stack)
    {
     this.node_id = node_id;
     this.var_name = var_name;
     this.parent = parent;
     this.continue_at_self = continue_at_self;
     this.call_stack = call_stack;

     this.sub_trees = new ArrayList<>();
    }

   public Def_tree(Long node_id, Object var_name, Def_tree parent, Boolean continue_at_self)
    {
     this(node_id, var_name, parent, continue_at_self, new ArrayList<Long>());
    }

   public Def_tree(Long node_id, Object var_name, Def_tree parent)
    {
     this(node_id, var_name, parent, false, new ArrayList<Long>());
    }


   public void add_sub_tree(Def_tree s)
    {
     this.sub_trees.add(s);
    }

   public List<Pair<Long, Object>> path_to_root()
    {
    List<Pair<Long, Object>> path = new ArrayList<>();
      if(parent != null)
       {
        path = parent.path_to_root();
       }
     path.add(new Pair<Long, Object>(node_id, var_name));
     return path;
    }

   public String toString()
    {
    String s = "[(";

     s += node_id.toString();
     s += ", ";
     s += var_name.toString();
      if(call_stack.size() != 0)
       {
        s += ", ";
        s += call_stack.toString();
       }
     s += "), [";
      for(Def_tree it : sub_trees)
       {
        s += it.toString();
       }
     s += "]]";
     return s;
    }
 } // EOF class

