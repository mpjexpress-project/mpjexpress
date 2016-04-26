package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class FencePut1Test {
  public static void main (String args[]) throws Exception {
    MPI.Init(args);

    int rank = MPI.COMM_WORLD.Rank();
    int size = MPI.COMM_WORLD.Size();

    ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 4);
    IntBuffer intbuffer = buffer.asIntBuffer();
    int[] sendbuffer = new int[2];

    sendbuffer[0] = rank;
    sendbuffer[1] = 0;

    Win win = Win.create(buffer, 4, MPI.COMM_WORLD);
    win.fence(MPI.MODE_NOPRECEDE | MPI.MODE_NOSTORE);
    win.put(sendbuffer, 1, MPI.INT, (rank + 1) % size, 1, 1, MPI.INT);
    win.fence(MPI.MODE_NOSUCCEED | MPI.MODE_NOPUT);

    if (intbuffer.get(1) != (rank + size - 1) % size) {
      throw new Exception("Put appears to have failed. " +
                          "Found " + intbuffer.get(1) +
                          ", expected " +
                          ((rank + size - 1) % size) + ".");
    }

    win.free();
    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
