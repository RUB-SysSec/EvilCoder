package tools.data_flow;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;

import java.util.Iterator;

class Best_effort_topological_sort<T extends Comparable<T>> implements Iterable<T>
 {
 private HashMap<T, Set<T>> data = null;
 private LinkedList<T> cur = null;
 private Topological_sort t = null;
 private int data_size;
 private int cur_index;
   public Best_effort_topological_sort(HashMap<T, Set<T>> data)
    {
     this.data = new HashMap<>(data);
     this.cur = new LinkedList<>();
     this.t = new Topological_sort(this.data);
     this.data_size = data.size();
     this.cur_index = 0;
    }

   private void remove_dep(T circle_element)
    {
    T key = data.keySet().iterator().next();
     data.put(circle_element, new HashSet<T>());
    }


   @Override
   public Iterator<T> iterator()
    {
    Iterator<T> it = new Iterator<T>()
     {
       @Override
       public boolean hasNext()
        {
         return cur_index < data_size;
        }

       @Override
       public T next()
        {
         cur_index += 1;
          if(cur.size() != 0)
           {
            return cur.pollFirst();
           }

          try
           {
            cur = new LinkedList<>(t.next_elements());
            return next();
           }
          catch(Circle_exception e)
           {
            remove_dep((T)(e.circle_element));
            t = new Topological_sort(data);
            return next();
           }
        }

       @Override
       public void remove()
        {
         throw new UnsupportedOperationException();
        }
     };
     return it;
    }

   public static void self_test()
    {
    HashMap<Long, Set<Long>> test_data = new HashMap<>();
     test_data.put(new Long(1), new HashSet<Long>());
     test_data.put(new Long(2), new HashSet<Long>());
     test_data.put(new Long(3), new HashSet<Long>());
     test_data.put(new Long(4), new HashSet<Long>());

     test_data.get(new Long(1)).add(new Long(2));
     test_data.get(new Long(3)).add(new Long(4));
     test_data.get(new Long(4)).add(new Long(3));

    Best_effort_topological_sort tp = new Best_effort_topological_sort<Long>(test_data);
      for(Iterator<Long> iter = tp.iterator(); iter.hasNext();)
       {
// Should be 2, 1, 3, 4 or 2, 1, 4, 3
       Long it = iter.next();
        System.out.println(it);
       }
    }
 }
