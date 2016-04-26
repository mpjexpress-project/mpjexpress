package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class GetTest {
  public static void main (String args[]) throws Exception {
    MPI.Init(args);

    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer winArea = ByteBuffer.allocateDirect(1 * 4);
    IntBuffer winAreaInt = winArea.asIntBuffer();
    int[][] rcvArea = new int[size][1];

    Win win = Win.create(winArea, 4, MPI.COMM_WORLD);

    /* Have every assign their "get" area to be their rank value */
    winAreaInt.put(0, rank);
    win.fence(0);

    /* Have everyone get from everyone else */
    for (int i = 0; i < size; ++i) {
      rcvArea[i][0] = -1;
      win.get(rcvArea[i], 1, MPI.INT, i, 0, 1, MPI.INT);
    }
    win.fence(0);

    /* Check to see that we got the right value */
    for (int i = 0; i < size; ++i)
      if (rcvArea[i][0] != i)
        throw new Exception("Rank " + rank + " got rcvArea[" +
                            i + "] = " + rcvArea[i][0] +
                            " when expecting " + i + "\n");

    win.free();

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");

  }
}
