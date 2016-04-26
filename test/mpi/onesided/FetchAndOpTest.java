package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 */

import java.nio.*;

import mpi.*;

public class FetchAndOpTest {
	private static int ITER = 100;
	private static int[] errors = new int[1];
	private static int[] allErrors = new int[1];

	public static void main(String[] args) throws MPIException {
		int rank, nproc;
		
		errors[0] = 0;
		allErrors[0] = 0;

		MPI.Init(args);

		rank = MPI.COMM_WORLD.Rank();
		nproc = MPI.COMM_WORLD.Size();

		ByteBuffer valPtrRaw = ByteBuffer.allocateDirect(nproc * 4);
		ByteBuffer resPtrRaw = ByteBuffer.allocateDirect(nproc * 4);
		Win win;
		IntBuffer valPtr = valPtrRaw.asIntBuffer();
		IntBuffer resPtr = resPtrRaw.asIntBuffer();

		win = Win.create(valPtrRaw, 4, MPI.COMM_WORLD);

		selfComm(valPtr, resPtr, win, rank);
		neighborComm(valPtr, resPtr, win, rank, nproc);
		contention(valPtr, resPtr, win, rank, nproc);
		allToAllCommFence(valPtr, resPtr, win, rank, nproc);
		allToAllCommLockAll(valPtr, resPtr, win, rank, nproc);
		allToAllCommLockAllFlush(valPtr, resPtr, win, rank, nproc);
		noOpNeighbor(valPtr, resPtr, win, rank, nproc);
		noOpSelf(valPtr, resPtr, win, rank, nproc);

		win.free();

		MPI.COMM_WORLD.Reduce(errors, 0, allErrors, 0, 1, MPI.INT, MPI.SUM, 0);

		MPI.Finalize();

		if (rank == 0 && allErrors[0] == 0)
			System.out.printf("Test successful\n");
	}

	// Test self communication
	private static void selfComm(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank) throws MPIException {
		resetVars(valPtr, resPtr, win);

		for (int i = 0; i < ITER; i++) {
			int[] one = new int[1];
			int[] result = new int[1];
			one[0] = 1;
			result[0] = -1;

			win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
			win.fetchAndOp(one, result, MPI.INT, rank, 0, MPI.SUM);
			win.unlock(rank);
		}

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		if ( CMP(valPtr.get(0), ITER) ) {
			System.out.println("selfComm " + rank + "->" + rank + " -- SELF: expected " + ITER + ", got " + valPtr.get(0) + "\n");
			errors[0]++;
		}
		win.unlock(rank);
	}

	// Test neighbor communication
	private static void neighborComm(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		resetVars(valPtr, resPtr, win);

		for (int i = 0; i < ITER; i++) {
			int[] one = new int[1];
			int[] result = new int[1];
			one[0] = 1;
			result[0] = -1;

			win.lock(MPI.LOCK_EXCLUSIVE, (rank + 1) % nproc, 0);
			win.fetchAndOp(one, result, MPI.INT, (rank + 1) % nproc, 0, MPI.SUM);
			win.unlock((rank + 1) % nproc);
			if ( CMP(result[0], i) ) {
				System.out.println("neighborComm " + ((rank + 1) % nproc) + "->" + rank + " -- NEIGHBOR[" + i + "]: expected result "
				                   + i + ", got " + result[0] + "\n");
				errors[0]++;
			}
		}

		MPI.COMM_WORLD.Barrier();

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		if ( CMP(valPtr.get(0), ITER) ) {
			System.out.println("neighborComm2 " + ((rank + 1) % nproc) + "->" + rank + " -- NEIGHBOR: expected " + ITER +
			                   ", got " + valPtr.get(0) + "\n");
			errors[0]++;
		}
		win.unlock(rank);
	}

	// Test contention
	private static void contention(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		resetVars(valPtr, resPtr, win);

		if (rank != 0) {
			for (int i = 0; i < ITER; i++) {
				int[] one = new int[1];
				int[] result = new int[1];
				one[0] = -1;

				win.lock(MPI.LOCK_EXCLUSIVE, 0, 0);
				win.fetchAndOp(one, result, MPI.INT, 0, 0, MPI.SUM);
				win.unlock(0);
			}
		}

		MPI.COMM_WORLD.Barrier();

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		if (rank == 0 && nproc > 1) {
			if ( CMP(valPtr.get(0), ITER * (nproc - 1)) ) {
				System.out.println("contention *->" + rank + " - CONTENTION: expected=" + (ITER * (nproc - 1)) + " val=" + valPtr.get(0) + "\n");
				errors[0]++;
			}
		}
		win.unlock(rank);
	}

	//Test all-to-all communication (fence)
	private static void allToAllCommFence(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		resetVars(valPtr, resPtr, win);

		int[][] result = new int[nproc][1];
		for (int i = 0; i < nproc; i++) {
			result[i][0] = -1;
		}

		for (int i = 0; i < ITER; i++) {
			win.fence(MPI.MODE_NOPRECEDE);

			for (int j = 0; j < nproc; j++) {
				int[] rankCNV = new int[1];
				rankCNV[0] = rank;

				win.fetchAndOp(rankCNV, result[j], MPI.INT, j, rank, MPI.SUM);
				result[j][0] = (i * rank);
			}
			win.fence(MPI.MODE_NOSUCCEED);
			MPI.COMM_WORLD.Barrier();

			for (int j = 0; j < nproc; j++) {
				if (CMP(result[j][0], (i * rank))) {
					System.out.println("allToAllCommFence " + rank + "->" + j + " -- ALL-TO-ALL (FENCE) [" + i + "]: expected result " +
					                   (i * rank) + ", got " + result[j][0] + "\n");
					errors[0]++;
				}
			}
		}

		MPI.COMM_WORLD.Barrier();
		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		for (int i = 0; i < nproc; i++) {
			if (CMP(valPtr.get(i), (ITER * i))) {
				System.out.println("allToAllCommFence2 " + i + "->" + rank + " -- ALL-TO-ALL (FENCE): expected " + (ITER * i) +
				                   ", got " + valPtr.get(i) + "\n");
				errors[0]++;
			}
		}
		win.unlock(rank);
	}

	//Test all-to-all communication (lock-all)
	private static void allToAllCommLockAll(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		resetVars(valPtr, resPtr, win);

		int[][] result = new int[nproc][1];
		for (int i = 0; i < nproc; i++) {
			result[i][0] = -1;
		}

		for (int i = 0; i < ITER; i++) {
			int j;

			win.lockAll(0);
			for (j = 0; j < nproc; j++) {
				int[] rankCNV = new int[1];
				rankCNV[0] = rank;
				win.fetchAndOp(rankCNV, result[j], MPI.INT, j, rank, MPI.SUM);
				result[j][0] = i * rank;
			}
			win.unlockAll();
			MPI.COMM_WORLD.Barrier();

			for (j = 0; j < nproc; j++) {
				if (CMP(result[j][0], (i * rank))) {
					System.out.println("allToAllCommLockAll " + rank + "->" + j + " -- ALL-TO-ALL (LOCK-ALL) [" + i + "]: expected result " +
					                   (i * rank) + ", got " + (result[j][0]) + "\n");
					errors[0]++;
				}
			}
		}

		MPI.COMM_WORLD.Barrier();
		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		for (int i = 0; i < nproc; i++) {
			if (CMP(valPtr.get(i), (ITER * i))) {
				System.out.println("allToAllCommLockAll1 " + i + "->" + rank + " -- ALL-TO-ALL (LOCK-ALL): expected " + (ITER * i) +
				                   ", got " + valPtr.get(i) + "\n");
				errors[0]++;
			}
		}
		win.unlock(rank);
	}

	// Test all-to-all communication (lock-all+flush)
	private static void allToAllCommLockAllFlush(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		resetVars(valPtr, resPtr, win);

		int[][] result = new int[nproc][1];
		for (int i = 0; i < nproc; i++) {
			result[i][0] = -1;
		}

		for (int i = 0; i < ITER; i++) {
			win.lockAll(0);

			for (int j = 0; j < nproc; j++) {
				int[] rankCNV = new int[1];
				rankCNV[0] = rank;
				win.fetchAndOp(rankCNV, result[j], MPI.INT, j, rank, MPI.SUM);
				result[j][0] = i * rank;
				win.flush(j);
			}
			win.unlockAll();
			MPI.COMM_WORLD.Barrier();

			for (int j = 0; j < nproc; j++) {
				if (CMP(result[j][0], (i * rank))) {
					System.out.println("allToAllCommLockAllFlush " + rank + "->" + j + " -- ALL-TO-ALL (LOCK-ALL+FLUSH) [" + i +
					                   "]: expected result " + (i * rank) + ", got " + result[j][0] + "\n");
					errors[0]++;
				}
			}
		}

		MPI.COMM_WORLD.Barrier();
		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		for (int i = 0; i < nproc; i++) {
			if (CMP(valPtr.get(i), (ITER * i))) {
				System.out.println("allToAllCommLockAllFlush1 " + i + "-> " + rank + " -- ALL-TO-ALL (LOCK-ALL+FLUSH): expected " + (ITER * i) +
				                   ", got " + valPtr.get(i) + "\n");
				errors[0]++;
			}
		}
		win.unlock(rank);
	}

	//Test NO_OP (neighbor communication)
	private static void noOpNeighbor(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		//can not use null in fetchAndOp
		// int[] nullPtr = new int[0];
		// int[] result = new int[1];
		// result[0] = -1;

		// MPI.COMM_WORLD.Barrier();
		// resetVars(valPtr, resPtr, win);

		// win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		// for (int i = 0; i < nproc; i++)
		// 	valPtr.put(i, rank);
		// win.unlock(rank);
		// MPI.COMM_WORLD.Barrier();

		// for (int i = 0; i < ITER; i++) {
		// 	int target = (rank + 1) % nproc;

		// 	win.lock(MPI.LOCK_EXCLUSIVE, target, 0);
		// 	win.fetchAndOp(nullPtr, result, MPI.INT, target, 0, MPI.NO_OP);
		// 	win.unlock(target);

		// 	if (result[0] != target) {
		// 		System.out.println("noOpNeighbor " + target + "->" + rank + " -- NOP[" + i + "]: expected " + target +
		// 		                   ", got " + result[0] + "\n");
		// 		errors[0]++;
		// 	}
		// }
	}

	// Test NO_OP (self communication)
	private static void noOpSelf(IntBuffer valPtr, IntBuffer resPtr, Win win, int rank, int nproc) throws MPIException {
		//can not use null in fetchAndOp
		// int[] nullPtr = new int[0];
		// int[] result = new int[1];
		// result[0] = -1;

		// MPI.COMM_WORLD.Barrier();
		// resetVars(valPtr, resPtr, win);

		// win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);
		// for (int i = 0; i < nproc; i++)
		// 	valPtr.put(i, rank);
		// win.unlock(rank);
		// MPI.COMM_WORLD.Barrier();

		// for (int i = 0; i < ITER; i++) {
		// 	int target = rank;

		// 	win.lock(MPI.LOCK_EXCLUSIVE, target, 0);
		// 	win.fetchAndOp(nullPtr, result, MPI.INT, target, 0, MPI.NO_OP);
		// 	win.unlock(target);

		// 	if (result[0] != target) {
		// 		System.out.println("noOpSelf " + target + "->" + rank + " -- NOP_SELF[" + i + "]: expected " + target +
		// 		                   ", got " + result[0] + "\n");
		// 		errors[0]++;
		// 	}
		// }
	}

	private static void resetVars(IntBuffer valPtr, IntBuffer resPtr, Win win) throws MPIException {
		int rank, nproc;

		rank = MPI.COMM_WORLD.Rank();
		nproc = MPI.COMM_WORLD.Size();

		win.lock(MPI.LOCK_EXCLUSIVE, rank, 0);

		for (int i = 0; i < nproc; i++) {
			valPtr.put(i, 0);
			resPtr.put(i, -1);
		}
		win.unlock(rank);

		MPI.COMM_WORLD.Barrier();
	}

	private static boolean CMP(int x, int y) {
		return (x - y) > 0.000000001;
	}
}
