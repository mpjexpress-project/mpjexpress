package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class CreateInfoHalfTest {
  public static void main (String args[]) throws Exception {
    MPI.Init(args);

    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    // ByteBuffer buffer = ByteBuffer.allocateDirect(1);

    // Info info = new Info();
    // if (rank % 2 != 0) {
    //   info.set("no_locks", "false");
    // }
    // Win win = Win.create(buffer, 1, info, MPI.COMM_WORLD);
    // win.free();
    // info.free();
    MPI.Finalize();
    if (rank == 0)
      System.out.println("Test skipped.");
      // System.out.println("Test successful.");
  }
}
