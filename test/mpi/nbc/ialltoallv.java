import mpi.*;
import java.lang.*;
import java.io.*;
public class ialltoallv{

    
    public static void main(String args[]) throws Exception {


    final int MAXLEN = 10;

    int myself, tasks;
    MPI.Init(args);
    myself = MPI.COMM_WORLD.Rank();
    tasks = MPI.COMM_WORLD.Size();
    Request req = null;

    int root, i = 0, j, k, stride = 15;
    int out[] = new int[tasks * stride];
    int in[] = new int[tasks * stride];
    int sdis[] = new int[tasks];
    int scount[] = new int[tasks];
    int rdis[] = new int[tasks];
    int rcount[] = new int[tasks];
    int ans[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 15, 16, 17, 18,
	19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36,
	37, 38, 39, 40, 41, 42, 43, 44 };

    for (i = 0; i < tasks; i++) {
      sdis[i] = i * stride;
      scount[i] = 15;
      rdis[i] = i * 15;
      rcount[i] = 15;
    }

    if (myself == 0)
      for (i = 0; i < tasks; i++)
	scount[i] = 10;

    rcount[0] = 10;
    for (j = 0; j < tasks; j++)
      for (i = 0; i < stride; i++) {
	out[i + j * stride] = i + myself * stride;
	in[i + j * stride] = 0;
      }

     req = MPI.COMM_WORLD.Ialltoallv(out, 0, scount, sdis, MPI.INT, in, 0, rcount,
	rdis, MPI.INT);
	req.Wait();

    
    
     
    MPI.COMM_WORLD.Barrier();

    if (myself == 0) {
      System.out.println("iAlltoallv TEST COMPLETE");
    }

    MPI.Finalize();

  }


}