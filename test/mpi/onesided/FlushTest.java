package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 *
 * INCOMPLETE - unimplemented mpi.replace
 */

import java.nio.*;
import mpi.*;

public class FlushTest {
  private static int ITER = 100;

  public static void main(String[] args) throws Exception {
    int nproc;
    ByteBuffer buf = ByteBuffer.allocateDirect(1 * 4);
    IntBuffer bufInt = buf.asIntBuffer();
    int[] rank = new int[1];
    int[] errors = new int[1];
    int[] allErrors = new int[1];

    errors[0] = 0;
    allErrors[0] = 0;

    Win window;

    MPI.Init(args);

    // rank[0] = MPI.COMM_WORLD.Rank();
    // nproc = MPI.COMM_WORLD.Size();

    // if (nproc < 2) {
    //   if (rank[0] == 0)
    //     System.out.printf("Error: must be run with two or more processes\n");
    //   return;
    // }

    // /** Create using MPI_Win_create() **/

    // if (rank[0] == 0) {
    //   bufInt.put(0, (nproc - 1));
    // }

    // if (rank[0] == 0) {
    //   window = Win.create(buf, 4, MPI.COMM_WORLD);
    // } else {
    //   window = Win.create(buf, 4, MPI.COMM_WORLD);
    // }

    // /* Test flush of an empty epoch */
    // window.lock(MPI.LOCK_SHARED, 0, 0);
    // window.flushAll();
    // window.unlock(0);

    // MPI.COMM_WORLD.Barrier();

    // /* Test third-party communication, through rank 0. */
    // window.lock(MPI.LOCK_SHARED, 0, 0);

    // for (int i = 0; i < ITER; i++) {
    //   int[] val = new int[1];
    //   val[0] = -1;
    //   int exp = -1;

    //    Processes form a ring.  Process 0 starts first, then passes a token
    //    * to the right.  Each process, in turn, performs third-party
    //    * communication via process 0's window. 
    //   if (rank[0] > 0) {
    //     MPI.COMM_WORLD.Recv(null, 0, 0, MPI.BYTE, (rank[0] - 1), 0);
    //   }

    //   window.getAccumulate(rank, 1, MPI.INT, val, 1, MPI.INT, 0, 0, 1, MPI.INT, MPI.REPLACE);
    //   window.flush(0);

    //   exp = (rank[0] + nproc - 1) % nproc;

    //   if (val[0] != exp) {
    //     System.out.printf("%d - Got %d, expected %d\n", rank, val, exp);
    //     errors[0]++;
    //   }

    //   if (rank[0] < nproc - 1) {
    //     MPI.COMM_WORLD.Send(null, 0, 0, MPI.BYTE, (rank[0] + 1), 0);
    //   }

    //   MPI.COMM_WORLD.Barrier();
    // }

    // window.unlock(0);

    // window.free();

    // MPI.COMM_WORLD.Reduce(errors, 0, allErrors, 0, 1, MPI.INT, MPI.SUM, 0);

    // if (rank[0] != 0 || allErrors[0] != 0)
    //   throw new Exception("There was an error in flushing.");

    MPI.Finalize();

    // if (rank[0] == 0)
      // System.out.println("Test successful.");
      System.out.println("Test skipped.");
  }
}