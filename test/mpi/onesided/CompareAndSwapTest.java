package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;
import mpi.*;

public class CompareAndSwapTest {
	private static int ITER = 100;

	public static void main(String[] args) throws Exception {
		int rank, nproc;
		int[] errors = new int[1];
		int[] allErrors = new int[1];
		// IntBuffer valPtr = MPI.newIntBuffer(1);
		ByteBuffer valPtr = ByteBuffer.allocateDirect(1 * 4);
		IntBuffer valPtrInt = valPtr.asIntBuffer();
		Win win;

		errors[0] = 0;
		allErrors[0] = 0;

		MPI.Init(args);

		rank = MPI.COMM_WORLD.Rank();
		nproc = MPI.COMM_WORLD.Size();

		valPtrInt.put(0, 0);

		win = Win.create(valPtr, 1, MPI.COMM_WORLD);

		/* Test self communication */

		for (int i = 0; i < ITER; i++) {
			int[] next = new int[1];
			int[] iBuffer = new int[1];
			int[] result = new int[1];

			next[0] = (i + 1);
			iBuffer[0] = i;
			result[0] = -1;

			win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
			win.compareAndSwap(next, iBuffer, result, MPI.INT, rank, 0);
			win.unlock(rank);

			if (result[0] != i) {
				errors[0]++;
				System.out.println("Self " + rank + "->" + rank + " -- Error: next=" + next[0] + " compare=" + iBuffer[0] +
				                    " result=" + result[0] + " val=" + valPtrInt.get(0) + "\n");
			}
		}

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		valPtrInt.put(0, 0);
		win.unlock(rank);

		MPI.COMM_WORLD.Barrier();

		/* Test neighbor communication */

		for (int i = 0; i < ITER; i++) {
			int[] next = new int[1];
			int[] iBuffer = new int[1];
			int[] result = new int[1];

			next[0] = (i + 1);
			iBuffer[0] = i;
			result[0] = -1;

			win.lock(MPI.LOCK_EXCLUSIVE, (rank + 1) % nproc, 0);
			win.compareAndSwap(next, iBuffer, result, MPI.INT, (rank + 1) % nproc, 0);
			win.unlock((rank + 1) % nproc);
			if (result[0] != i) {
				errors[0]++;
				System.out.println("neighbor " + rank + "->" + (rank + 1) % nproc + " -- Error: next=" + next[0] + " compare=" + iBuffer[0] +
				                    " result=" + result[0] + " val=" + valPtrInt.get(0) + "\n");
			}
		}

		MPI.COMM_WORLD.Barrier();

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		valPtrInt.put(0, 0);
		win.unlock(rank);
		MPI.COMM_WORLD.Barrier();

		/* Test contention */

		if (rank != 0) {
			for (int i = 0; i < ITER; i++) {
				int[] next = new int[1];
				int[] iBuffer = new int[1];
				int[] result = new int[1];

				next[0] = (i + 1);
				iBuffer[0] = i;
				result[0] = -1;

				win.lock(MPI.LOCK_EXCLUSIVE, 0, 0);
				win.compareAndSwap(next, iBuffer, result, MPI.INT, 0, 0);
				win.unlock(0);
			}
		}

		MPI.COMM_WORLD.Barrier();

		if (rank == 0 && nproc > 1) {
			if (valPtrInt.get(0) != ITER) {
				errors[0]++;
				System.out.println("contention " + rank + " - Error: expected=" + ITER + " val=" + valPtrInt.get(0) + "\n");
			}
		}

		win.free();

		MPI.COMM_WORLD.Reduce(errors, 0, allErrors, 0, 1, MPI.INT, MPI.SUM, 0);

		if (rank != 0 && allErrors[0] != 0)
			throw new Exception("Error in compare and swap.");

		MPI.Finalize();

		if (rank == 0)
			System.out.println("Test successful.");

	}

}