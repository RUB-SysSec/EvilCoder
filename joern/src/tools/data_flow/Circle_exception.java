package tools.data_flow;

class Circle_exception extends Exception
 {
    public Object circle_element;

    public Circle_exception(Object circle_element)
     {
      this.circle_element = circle_element;
     }
 }

