  import mpi.*;
import java.lang.*;
import java.io.*;
public class iallgatherv {
    public static void main(String args[]) throws Exception {
        MPI.Init(args);


  final int MAXLEN = 10;

    int root, i, j, k;
    int myself, tasks, stride = 15;

    MPI.Init(args);
    myself = MPI.COMM_WORLD.Rank();
    tasks = MPI.COMM_WORLD.Size();
    Request req = null;

    if (tasks > 8) {
      if (myself == 0) {
	System.out.println("allgatherv must run with less than 8 tasks! ");
      }
      MPI.Finalize();
      return;
    }

    int out[] = new int[MAXLEN];
    int in[] = new int[MAXLEN * stride * tasks];
    int dis[] = new int[MAXLEN];
    int rcount[] = new int[MAXLEN];
    int ans[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4,
	0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	0 };

    for (i = 0; i < MAXLEN; i++) {
      dis[i] = i * stride;
      rcount[i] = 5;
      out[i] = i;
    }
    rcount[0] = 10;

    for (i = 0; i < MAXLEN * tasks * stride; i++) {
      in[i] = 0;
    }

    if (myself == 0)
      req = MPI.COMM_WORLD.Iallgatherv(out, 0, 10, MPI.INT, in, 0, rcount, dis,
	  MPI.INT);
    else
      req = MPI.COMM_WORLD
	  .Iallgatherv(out, 0, 5, MPI.INT, in, 0, rcount, dis, MPI.INT);


   req.Wait();
    for (i = 0; i < tasks * stride; i++)
      if (ans[i] != in[i])
	System.out.println("recived data : " + in[i] + "at [" + i
	    + "] should be : " + ans[i] + " on proc. : " + myself);

    MPI.COMM_WORLD.Barrier();

    if (myself == 0) {
      System.out.println("test condition for allgatherv is disabled coz\n"
	  + "it throws out of bound exception when total\n"
	  + "processes are more than 3 (TBFixed) ");
      System.out.println("Allgatherv TEST COMPLETE");
    }

    MPI.Finalize();

  }

}