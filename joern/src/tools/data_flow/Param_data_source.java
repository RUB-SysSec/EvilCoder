package tools.data_flow;

class Param_data_source
 {
   public Long node_id;
   public Object var_name;
   public Boolean continue_at_self;

   public Param_data_source(Long node_id, Object var_name, Boolean continue_at_self)
    {
     this.node_id = node_id;
     this.var_name = var_name;
     this.continue_at_self = continue_at_self;
    }

   public String toString()
    {
    String s = "";
     s += "node_id:" + node_id.toString();
     s += "; " + var_name.toString();
     s += "; " + continue_at_self.toString(); 
     return s;
    }
 }

