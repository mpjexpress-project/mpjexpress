package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 *
 * INCOMPLETE ADAPTATION - unimplemented Info
 */

import java.nio.*;
import mpi.*;

public class LockIllegalTest {
	public static void main (String args[]) throws Exception {
		MPI.Init(args);

		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();

		/* MPI_Win_create needs sizeOfInt in bytes, so that we cannot use
		 * MPI.INT.getExtent() or we must adapt the size from elements
		 * to bytes in the Java API
		 */
		// Info info = new Info();
		// info.set("no_locks", "true");
		// ByteBuffer bufferRaw = ByteBuffer.allocateDirect(1 * 4);
		// IntBuffer buffer = bufferRaw.asIntBuffer();
		// Win win = Win.create(buffer, 4, info, MPI.COMM_WORLD);
		// win.setErrhandler(MPI.ERRORS_RETURN);

		// info.free();

		// if (rank == 0) {
		// 	win.lock(MPI.LOCK_SHARED, 0, 0);

		// 	throw new Exception("MPI_Win_lock succeeded when no_locks given");
		// }
		// win.free();

		MPI.Finalize();
		System.out.println("Test skipped.");
		// System.out.println("Test successful.");
	}
}
