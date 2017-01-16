package tools.data_flow;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class Topological_sort<T extends Comparable<T>>
 {
   private HashMap<T, Set<T>> data;
   public Topological_sort(HashMap<T, Set<T>> data)
    {
     this.data = data;
    }

   public List<T> next_elements() throws Circle_exception
    {
    List<T> leafs = new LinkedList<>();
    List<T> to_remove = new LinkedList<>();
      for(T node : data.keySet())
       {
         if(data.get(node).isEmpty())
          {
           leafs.add(node);
           to_remove.add(node);
          }
       }

      for(T node : to_remove)
       {
        data.remove(node);
       }

      if(leafs.isEmpty())
       {
       T min_node = null;
       int min_edges = Integer.MAX_VALUE;
         for(T node : data.keySet())
          {
          int its_size = data.get(node).size();
            if(its_size < min_edges)
             {
              min_edges = its_size;
              min_node = node;
             }
          }
        throw new Circle_exception(min_node);
       }

      for(T leaf : leafs)
       {
         for(T remaining : data.keySet())
          {
           data.get(remaining).remove(leaf);
          }
       }

     return leafs;
    }
 }

