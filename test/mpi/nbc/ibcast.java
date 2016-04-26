import mpi.*;
import java.lang.*;
import java.io.*;
public class ibcast {
    public static void main(String args[]) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();


        Request req = null;
        
        int unitSize = 4;
        int root = 0;
        int sendbuf[]= null;
        sendbuf = new int[unitSize];
  if (rank == root)
  {
   
   for(int i = 0; i <unitSize ; i++)
       sendbuf[i] = i;

  }
   req = MPI.COMM_WORLD.Ibcast(sendbuf, 0, unitSize, MPI.INT, root);
   req.Wait();


     for(int i = 0; i <unitSize ; i++)
     	System.out.print(sendbuf[i]+" ");


        MPI.Finalize();
    }
}