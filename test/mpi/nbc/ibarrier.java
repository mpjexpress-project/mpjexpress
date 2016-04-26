import mpi.*;
import java.lang.*;
import java.io.*;
public class ibarrier {
    public static void main(String args[]) throws Exception {


int me, tasks, i;

    MPI.Init(args);
    me = MPI.COMM_WORLD.Rank();
    tasks = MPI.COMM_WORLD.Size();
    Request req = null;

    if (tasks < 2) {
      System.out.println("MUST RUN WITH AT LEAST 2 TASKS");
      System.exit(0);
    }

    for (i = 0; i < 250000 * me; i++)
      ;

    System.out.println(" TASK " + me + " BEFORE BARRIER, TIME = "
	+ (MPI.Wtime()));

    req = MPI.COMM_WORLD.Ibarrier();
    req.Wait();

    System.out.println(" TASK " + me + " AFTER  BARRIER, TIME = "
	+ (MPI.Wtime()));

    if (me == 0)
      System.out.println("Barrier TEST COMPLETE");
    MPI.Finalize();

  }

}