package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class WinAttrTest {
  public static void main (String args[]) throws Exception {
    MPI.Init(args);
    int rank = MPI.COMM_WORLD.Rank();
    int sizeOfInt = 4;

    ByteBuffer buffer = ByteBuffer.allocateDirect(25 * 4);

    /* one integer, displacement of 1 integer */
    Win win = Win.allocate(4, 4, MPI.COMM_WORLD);

    Object value = win.getBase();
    if (value == null) {
      throw new Exception("Window Base was null");
    }

    value = win.getSize();
    if (value == null) {
      throw new Exception("Window Size was null");
    }

    if (sizeOfInt != ((Integer)value).intValue()) {
      throw new Exception("Window Size " + sizeOfInt +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    value = win.getDispUnit();

    if (value == null) {
      throw new Exception("Window Disp Unit was null");
    }

    if (sizeOfInt != ((Integer)value).intValue()) {
      throw new Exception("Window Disp Unit " + sizeOfInt +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    win.free();

    /* 25 integers, displacement of 1 integer */
    win = Win.create(buffer, 4, MPI.COMM_WORLD);
    value = win.getBase();
    if (value == null) {
      throw new Exception("Window Base was null");
    }

    value = win.getSize();
    if (value == null) {
      throw new Exception("Window Size was null");
    }

    if (sizeOfInt * 25 != ((Integer)value).intValue()) {
      throw new Exception("Window Size " + sizeOfInt * 25 +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    value = win.getDispUnit();

    if (value == null) {
      throw new Exception("Window Disp Unit was null");
    }

    if (sizeOfInt != ((Integer)value).intValue()) {
      throw new Exception("Window Disp Unit " + sizeOfInt +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    win.free();

    /* 25 integers, displacement of 1 integer */
    win = Win.create(buffer, 4, MPI.COMM_WORLD);

    value = win.getBase();
    if (value == null) {
      throw new Exception("Window Base was null");
    }

    value = win.getSize();

    if (value == null) {
      throw new Exception("Window Size was null");
    }

    if (sizeOfInt * 25 != ((Integer)value).intValue()) {
      throw new Exception("Window Size " + sizeOfInt * 25 +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    value = win.getDispUnit();

    if (value == null) {
      throw new Exception("Window Disp Unit was null");
    }

    if (sizeOfInt != ((Integer)value).intValue()) {
      throw new Exception("Window Disp Unit " + sizeOfInt +
                          "does not match returned value" +
                          ((Integer)value).intValue());
    }

    win.free();

    MPI.Finalize();

    if (rank == 0)
      System.out.println("Test successful.");
  }
}
