package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 * 
 * Todo UNIMPLEMENTED Test Handlers
 */

import java.nio.*;
import mpi.*;

public class WinErrhandlerTest {
  public static void main (String args[]) throws MPIException {
    // MPI.Init(args);

    // ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 4);
    // Win win = new Win(buffer, 4, MPI.INFO_NULL, MPI.COMM_WORLD);
    // win.setErrhandler(MPI.ERRORS_RETURN);
    // win.callErrhandler(MPI.ERR_OTHER);
    // /* success is not aborting ;) */
    // win.free();

    // MPI.Finalize();
    System.out.println("Skipped test.");
  }
}
