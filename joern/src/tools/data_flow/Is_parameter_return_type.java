package tools.data_flow;

class Is_parameter_return_type
 {
  public Boolean is_param;
  public String func_name;
  public Long param_index;

  public Is_parameter_return_type(Boolean is_param, String func_name, int param_index)
   {
    this.is_param = is_param;
    this.func_name = func_name;
    this.param_index = new Long(param_index);
   }
 }

