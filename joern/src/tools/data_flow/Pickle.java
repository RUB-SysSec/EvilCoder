package tools.data_flow;

import java.io.*;

public class Pickle
 {
   public static Boolean save_to_file(String file_name, Object to_write)
    {
      try
       {
       FileOutputStream fos = new FileOutputStream(file_name);
       ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(to_write);
        oos.close();
        fos.close();
        return true;
       }
      catch(IOException i)
       {
        i.printStackTrace();
        return false;
       }
    }

   public static Object load_from_file(String file_name)
    {
      try
       {
       FileInputStream fis = new FileInputStream(file_name);
       ObjectInputStream ois = new ObjectInputStream(fis);
       Object o = ois.readObject();
        ois.close();
        fis.close();
        return o;
       }
      catch(IOException i)
       {
        i.printStackTrace();
        return null;
       }
      catch(ClassNotFoundException c)
       {
        System.out.println("Class not found:");
        c.printStackTrace();
        return null;
       }
    }
 }
