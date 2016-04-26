import mpi.*;
import java.lang.*;
import java.io.*;
public class igather{
    public static void main(String args[]) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();


        Request req = null;
        
        int unitSize = 4;
        int root = 0;
        int sendbuf[]= null;
        sendbuf = new int[unitSize];
        int recvbuf[] = null;
        
        if (rank == root)
        {

             recvbuf = new int[unitSize*size];
 
        }

        for(int i = 0; i <unitSize ; i++)
         {
             sendbuf[i] = i;
             System.out.print("I am sendbuf "+ sendbuf[i] +" \n");
          }



      
         req = MPI.COMM_WORLD.Igather(sendbuf,0, unitSize, MPI.INT, recvbuf,0, unitSize, MPI.INT, root);
         req.Wait();

      
               
     if (rank == root)
     {
         for(int i = 0; i <unitSize*size ; i++)
         {
            
             System.out.print("I am recvbuf "+ recvbuf[i] +" \n");
          }
      }
      


        MPI.Finalize();
    }
}