package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class AccumulateTest {
  public static void main (String args[]) throws Exception {
    int rank, size, expected, i;

    MPI.Init(args);
    rank = MPI.COMM_WORLD.Rank();
    size = MPI.COMM_WORLD.Size();

    ByteBuffer recvBufRaw = ByteBuffer.allocateDirect(4);
    IntBuffer recvBuf = recvBufRaw.asIntBuffer();
    int[] sendBuf = new int[1];


    Win win = Win.create(recvBufRaw, 1, MPI.COMM_WORLD);
    sendBuf[0] = rank + 100;
    recvBuf.put(0, 0);

    /* Accumulate to everyone, just for the heck of it */
    win.fence(MPI.MODE_NOPRECEDE);
    for (i = 0; i < size; ++i)
      win.accumulate(sendBuf, 1, MPI.INT, i, 0, 1, MPI.INT, MPI.SUM);
    win.fence(MPI.MODE_NOPUT | MPI.MODE_NOSUCCEED);

    for (expected = 0, i = 0; i < size; i++)
      expected += (i + 100);
    if (recvBuf.get(0) != expected)
      throw new Exception("Rank " + rank + " got " + recvBuf.get(0) +
                          " when it expected " + expected + "\n");

    win.free();
    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
