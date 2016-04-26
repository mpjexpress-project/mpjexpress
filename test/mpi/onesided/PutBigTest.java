package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class PutBigTest {
  private final static int MSG_SIZE = 1024 * 256;

  public static void main (String args[]) throws Exception {
    MPI.Init(args);

    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer winArea = ByteBuffer.allocateDirect(size * MSG_SIZE * 4);
    IntBuffer winAreaInt = winArea.asIntBuffer();
    int[] putVals = new int[MSG_SIZE];

    Win win = Win.create(winArea, 4, MPI.COMM_WORLD);

    /* Set all the target areas to be -1 */
    for (int i = 0 ; i < MSG_SIZE ; ++i)
      putVals[i] = rank;
    for (int i = 0; i < size * MSG_SIZE; ++i)
      winAreaInt.put(i, -1);
    win.fence(0);

    /* Do a put to all other processes */
    for (int i = 0; i < size; ++i) {
      win.put(putVals, MSG_SIZE, MPI.INT, i,
              rank * MSG_SIZE, MSG_SIZE, MPI.INT);
    }
    win.fence(0);

    win.free();

    /* Check to see that we got the right values */
    for (int i = 0; i < size; ++i)
      for (int j = 0 ; j < MSG_SIZE ; ++j)
        if (winAreaInt.get(i * MSG_SIZE + j) != i)
          throw new Exception("Rank " + rank + " got winArea[" +
                              (i * MSG_SIZE + j) + " ]=" +
                              winAreaInt.get(i * MSG_SIZE + j) +
                              " when expecting " + i + "\n");

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
