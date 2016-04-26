package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 *
 * Todo Add tests for shared memory allocation if it is implemented.
 */

import java.nio.*;
import mpi.*;

public class WinAllocateTest {

	private static final int BASE_SIZE = 8192;

	public static void main(String[] args) throws MPIException {
		Win win/*, sharedWin*/;
		int rank, size, shmRank, /*shmNproc,*/ peer;
		int mySize, peer_size;
		int peer_disp;
		// Comm shmComm;

		MPI.Init(args);
		// MPI.COMM_WORLD.setErrhandler(MPI.ERRORS_RETURN);
		rank = MPI.COMM_WORLD.Rank();
		size = MPI.COMM_WORLD.Size();

		// shmComm = MPI.COMM_WORLD.splitType(Comm.TYPE_SHARED, rank, MPI.INFO_NULL);

		shmRank = MPI.COMM_WORLD.Rank();
		// shmNproc = MPI.COMM_WORLD.getSize();

		mySize = BASE_SIZE + (shmRank + 1);

		win = Win.allocate(mySize, 1, MPI.COMM_WORLD);
		// win = new Win(mySize, 1, MPI.INFO_NULL, shmComm, myPtr, Win.FLAVOR_PRIVATE);
		// sharedWin = new Win(mySize, 1, MPI.INFO_NULL, shmComm, myPtr, Win.FLAVOR_SHARED);

		win.free();
		// sharedWin.free();
		// shmComm.free();

		MPI.Finalize();

		if (rank == 0)
			System.out.println("Test successful.");
	}
}