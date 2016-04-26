package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class AccumulateAtomicTest {
  private final static int WINDOW_SIZE = 128;
  private final static int REPS = 100;

  public static void main (String args[]) throws Exception {
    int rank, size;

    MPI.Init(args);
    rank = MPI.COMM_WORLD.Rank();
    size = MPI.COMM_WORLD.Size();

    ByteBuffer rawbuffer = ByteBuffer.allocateDirect(WINDOW_SIZE * 2 * 4);
    // rawbuffer.order(ByteOrder.nativeOrder());
    IntBuffer buffer = rawbuffer.asIntBuffer();
    int[] sendBuf = new int[WINDOW_SIZE * 2];

    /* initialize bottom half to 1 and top half to 0 */
    for (int i = 0 ; i < WINDOW_SIZE ; ++i) {
      buffer.put(i, 1);
      buffer.put(i + WINDOW_SIZE, 0);
      sendBuf[i] = 1;
      sendBuf[i + WINDOW_SIZE] = 0;
    }

    /* create window */
    Win win = Win.create(rawbuffer, 4, MPI.COMM_WORLD);

    /* everyone updates root's upper half REPS * 2 times */
    win.fence(MPI.MODE_NOPRECEDE | MPI.MODE_NOSTORE);

    for (int i = 0 ; i < REPS ; ++i) {
      win.accumulate(sendBuf, WINDOW_SIZE, MPI.INT,
                     0, WINDOW_SIZE, WINDOW_SIZE, MPI.INT, MPI.SUM);
    }

    win.fence(0);


    // if (rank == 0) {
    //   for (int i = 0; i < WINDOW_SIZE * 2; i++) {
    //     System.out.print(buffer.get(i) + " ");
    //   }
    // }

    for (int i = 0 ; i < REPS ; ++i) {
      win.accumulate(sendBuf, WINDOW_SIZE, MPI.INT,
                     0, WINDOW_SIZE, WINDOW_SIZE, MPI.INT, MPI.SUM);
    }

    win.fence(MPI.MODE_NOSUCCEED | MPI.MODE_NOPUT);

    if (rank == 0) {
      for (int i = 0; i < WINDOW_SIZE * 2; i++) {
        System.out.print(buffer.get(i) + " ");
      }
    }



    /* check result */
    for (int i = 0 ; i < WINDOW_SIZE ; ++i) {
      if (buffer.get(i) != 1) {
        throw new Exception("Accumulate appears to have " +
                            "failed. Found " + buffer.get(i) +
                            " at " + i + ", expected 1.\n");
      }
    }

    if (rank == 0) {
      for (int i = WINDOW_SIZE ; i < WINDOW_SIZE * 2 ; ++i) {
        if (buffer.get(i) != size * REPS * 2) {
          throw new Exception("Accumulate appears to have " +
                              "failed. Found " + buffer.get(i) +
                              " at " + i + ", expected " +
                              (size * REPS * 2) + ".\n");
        }
      }
    }

    /* cleanup */
    win.free();
    //MPI_Free_mem(buffer);
    MPI.Finalize();
  }
}
