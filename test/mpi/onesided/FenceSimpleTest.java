package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class FenceSimpleTest {
	public static void main (String args[]) throws MPIException {
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 4);
		Win win = Win.create(buffer, 1, MPI.COMM_WORLD);
		win.fence(0);
		win.free();
		MPI.Finalize();

		if (rank == 0)
			System.out.println("Test successful.");
	}
}
