package tools.argumentTainter;
import java.util.Vector;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

// Parse command line and hand over to to ArgumentTainter

public class ArgumentTainterMain
 {
 static Vector<String> sources = new Vector<String>();
 static Vector<Integer> tainted_args = new Vector<Integer>();
 static String databaseDir;

 public static void main(String[] args) throws Exception
  {
   parseCommandLine(args);

  ArgumentTainter argumentTainter = new ArgumentTainter();
   argumentTainter.initialize(databaseDir);

    for(int i=0; i<sources.size(); ++i)
     {
      System.out.println("Tainting function " + String.valueOf(i+1) + " of " + String.valueOf(sources.size()) + ": " + sources.get(i) + "\t" + String.valueOf(tainted_args.get(i)));
      argumentTainter.setSourceToPatch(sources.get(i));
      argumentTainter.setArgToPatch(tainted_args.get(i));
      argumentTainter.patch();
     }
   argumentTainter.shutdown();
  }

 private static void parseCommandLine(String[] args) throws Exception
  {
    if(args.length != 2)
     {
      System.out.println("[/] Usage: " + args[0] + " <file_name> <database_dir>\n");
      System.out.println("[/] <file_name> holds lines, which contain <source> <tainted_arg>\n");
      System.exit(1);
     }

  String file_name = args[0];
   databaseDir = args[1];
    while(databaseDir.endsWith("/") && databaseDir.length() > 0)
    {
     databaseDir = databaseDir.substring(0, databaseDir.length()-1);
    }

   if(!databaseDir.endsWith(".joernIndex"))
    {
     System.err.println("[-] DatabaseDir has to end with .joernIndex");
     System.exit(0);
    }
          
  FileInputStream fstream = new FileInputStream(file_name);
  BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
  String line;
    while((line = br.readLine()) != null)
     {
      line = line.trim();
       if(line.equals(""))
        {
         continue;
        }
    
     String[] splitters = line.split("\t");
       if(splitters.length != 2)
        {
         System.err.println("[-] Expected two splitters per line");
         System.exit(0);
        }
      sources.add(splitters[0]);
      tainted_args.add(Integer.parseInt(splitters[1]));
     }
   br.close();
  }

} // EOF class

