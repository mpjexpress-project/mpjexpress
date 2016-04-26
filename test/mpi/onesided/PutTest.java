package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class PutTest {
  public static void main (String args[]) throws Exception {
    MPI.Init(args);
    
    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer winArea = ByteBuffer.allocateDirect(size * 4);
    IntBuffer winAreaInt = winArea.asIntBuffer();
    int[][] putVals = new int[size][1];

    Win win = Win.create(winArea, 4, MPI.COMM_WORLD);

    /* Set all the target areas to be -1 */
    for (int i = 0; i < size; ++i) {
      winAreaInt.put(i, -1);
      putVals[i][0] = rank;
    }
    win.fence(0);

    /* Do a put to all other processes */
    for (int i = 0; i < size; ++i)
      win.put(putVals[i], 1, MPI.INT, i, rank, 1, MPI.INT);
    win.fence(0);

    /* Check to see that we got the right values */
    for (int i = 0; i < size; ++i)
      if (winAreaInt.get(i) != i)
        throw new Exception("Rank " + rank + " got winArea[" +
                            i + " ]=" + winAreaInt.get(i) +
                            " when expecting " + i + "\n");

    win.free();

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");

  }
}
