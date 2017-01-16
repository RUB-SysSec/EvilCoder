package tools.data_flow;

class Arg_in_call_return_type
 {
  public Boolean is_arg;
  public String func_name;
  public Long arg_index;
  public Long nof_args;

  public Arg_in_call_return_type(Boolean is_arg, String func_name, int arg_index, int nof_args)
   {
    this.is_arg = is_arg;
    this.func_name = func_name;
    this.arg_index = new Long(arg_index);
    this.nof_args = new Long(nof_args);
   }

 }

