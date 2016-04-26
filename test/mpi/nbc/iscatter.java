import mpi.*;
import java.lang.*;
import java.io.*;
public class iscatter {
    public static void main(String args[]) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();


        Request req = null;
        
        int unitSize = 4;
        int root = 0;
        int sendbuf[]= null;
        
        if (rank == root)
        {
          sendbuf = new int[unitSize*size];
         
         for(int i = 0; i <unitSize*size ; i++)
         {
             sendbuf[i] = i;
             System.out.print("I am sendbuffer "+ sendbuf[i] +" \n");
          }


        }


         int recvbuf[]= new int[unitSize];
         req = MPI.COMM_WORLD.Iscatter(sendbuf,0, unitSize, MPI.INT, recvbuf,0, unitSize, MPI.INT, root);
         req.Wait();

      
         for(int i = 0; i < unitSize ; i++)
         System.out.print("I am recvbuffer "+recvbuf[i]+" \n");
      


        MPI.Finalize();
    }
}