
import mpi.*;
import java.lang.*;
import java.io.*;
public class ireduce{

    
    public static void main(String args[]) throws Exception {

    final int MAXLEN = 4;

    int root, i, j, k;
    int out[] = new int[MAXLEN];
    int in[] = new int[MAXLEN];
    int myself, tasks;

    MPI.Init(args);


    myself = MPI.COMM_WORLD.Rank();
    tasks = MPI.COMM_WORLD.Size();
      Request req = null;

    root = 0;

    if(myself == root)
    {
   
      for (i = 0; i < MAXLEN; i++)
      {

	       out[i] = 2;

	    }
    }

  if(myself != root)
    {

    for (i = 0; i < MAXLEN; i++)
      {

         out[i] = i;

      }
    }

      req = MPI.COMM_WORLD.Ireduce(out, 0, in, 0, MAXLEN, MPI.INT, MPI.SUM, root);
      req.Wait();

      if (myself == root) {
	for (k = 0; k < MAXLEN; k++) {
		System.out.println("answer: "+in[k]);
	  
	}
  }


    MPI.COMM_WORLD.Barrier();
    if (myself == 0)
      System.out.println("Reduce TEST COMPLETE");
    MPI.Finalize();

}

}