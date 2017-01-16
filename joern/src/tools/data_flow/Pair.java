package tools.data_flow;

public class Pair<FIRST, SECOND> implements Comparable<Pair<FIRST, SECOND>>, java.io.Serializable
 {
  public FIRST first;
  public SECOND second;

  public Pair(FIRST first, SECOND second)
   {
    this.first = first;
    this.second = second;
   }

  private static int compare_helper(Object o1, Object o2)
   {
    return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1 : ((Comparable)o1).compareTo(o2);
   }

  @Override
  public int compareTo(Pair<FIRST, SECOND> o)
   {
   int cmp = compare_helper(first, o.first);
    return cmp == 0 ? compare_helper(second, o.second) : cmp;
   }

  private static int hashcode_helper(Object o)
   {
    return o == null ? 0 : o.hashCode();
   }

  @Override
  public int hashCode()
   {
    return 65497 * hashcode_helper(first) ^ hashcode_helper(second);
   }

  private boolean equal_helper(Object o1, Object o2)
   {
    return o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2));
   }

  @Override
  public boolean equals(Object obj)
   {
     if(!(obj instanceof Pair)) return false;
     if (this == obj) return true;
    return equal_helper(first, ((Pair) obj).first) && equal_helper(second, ((Pair) obj).second);
   }

  @Override
  public String toString()
   {
    return "(" + first.toString() + ", " + second.toString() + ')';
   }
 } // EOF class
