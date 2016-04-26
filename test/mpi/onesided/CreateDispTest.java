package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class CreateDispTest {
  public static void main (String args[]) throws MPIException {
    MPI.Init(args);
    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer buffer = ByteBuffer.allocateDirect(1);

    Win win = Win.create(buffer, (rank % 4) + 1, MPI.COMM_WORLD);

    win.free();

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
