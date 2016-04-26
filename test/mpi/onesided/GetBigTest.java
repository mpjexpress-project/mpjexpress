package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class GetBigTest {
  private final static int MSG_SIZE = 1024 * 256;

  public static void main (String args[]) throws Exception {
    MPI.Init(args);

    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer winArea = ByteBuffer.allocateDirect(MSG_SIZE * 4);
    IntBuffer winAreaInt = winArea.asIntBuffer();
    int[][] rcvArea = new int[size][MSG_SIZE];

    Win win = Win.create(winArea, 4, MPI.COMM_WORLD);

    /* Have every assign their "get" area to be their rank value */
    for (int i = 0 ; i < MSG_SIZE ; ++i) {
      winAreaInt.put(i, rank);
    }

    /* Have everyone get from everyone else */
    win.fence(0);
    for (int i = 0; i < size; ++i) {
      for (int j = 0 ; j < MSG_SIZE ; ++j) {
        rcvArea[i][j] = -1;
      }
      win.get(rcvArea[i],
              MSG_SIZE, MPI.INT, i, 0, MSG_SIZE, MPI.INT);
    }
    win.fence(0);

    /* Check to see that we got the right value */
    for (int i = 0; i < size; ++i) {
      for (int j = 0 ; j < MSG_SIZE ; ++j) {
        if (rcvArea[i][j] != i) {
          throw new Exception("Rank " + rank + " got rcvArea[" +
                              i + "][ " + j + "] = " +
                              rcvArea[i][j] +
                              " when expecting " + i + "\n");
        }
      }
    }

    win.free();

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
