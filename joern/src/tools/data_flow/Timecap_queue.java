package tools.data_flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.LinkedList;


class Timecap_queue
 {
   private LinkedList<Retrace_arg_till_source> task_list;
   private int timecap_in_seconds;
   public Timecap_queue(LinkedList<Retrace_arg_till_source> task_list, int nof_threads, int timecap_in_seconds) throws Exception
    {
      if(nof_threads > 1)
       {
        throw new Exception("nof_threads > 1 is not implemented");
       }
     this.task_list = task_list;
     this.timecap_in_seconds = timecap_in_seconds;
    }

   public void start() throws Exception
    {
    ExecutorService executor = Executors.newSingleThreadExecutor();

      while(!task_list.isEmpty())
       {
       Retrace_arg_till_source it = task_list.pollFirst();

       Future<Void> future = executor.submit(it);
   
         try
          {
           future.get(timecap_in_seconds, TimeUnit.SECONDS);
          }
         catch(TimeoutException e)
          {
           future.cancel(true);
           System.out.println("[-] Premature termination of " + it.task_id);
          }
       }

     executor.shutdownNow();
    }
 }


