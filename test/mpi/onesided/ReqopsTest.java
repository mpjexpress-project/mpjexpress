package mpi.onesided;

/**
 * This code was adapted from https://github.com/open-mpi/ompi-java-test
 *
 * INCOMPLETE - unimplemented MPI.REPLACE
 */

import java.nio.*;

import mpi.*;

public class ReqopsTest {
	private static final int ITER = 100;
	private static int nproc;
	private static int[] errors = new int[1];
	private static int[] all_errors = new int[1];

	public static void main(String args[]) throws MPIException {
		errors[0] = 0;
		all_errors[0] = 0;
		Win window;
		ByteBuffer bufRaw = ByteBuffer.allocateDirect(0);
		IntBuffer buf = bufRaw.asIntBuffer();

		MPI.Init(args);
		// int[] rank = new int[1];
		// rank[0] = MPI.COMM_WORLD.Rank();
		// nproc = MPI.COMM_WORLD.Size();

		// if (nproc < 2) {
		// 	if (rank[0] == 0)
		// 		System.out.print("Error: must be run with two or more processes\n");
		// 	return;
		// }

		// /** Create using MPI_Win_create() **/

		// if (rank[0] == 0) {
		// 	bufRaw = ByteBuffer.allocateDirect(4 * 4);
		// 	buf = bufRaw.asIntBuffer();
		// 	buf.put(0, (nproc - 1));
		// }

		// window = Win.create(bufRaw, 4, MPI.COMM_WORLD);

		// procNullComm(window);
		// getACC(window, rank, buf);
		// getAndPut(window, rank, buf);
		// getAndACC(window, rank);
		// waitInEpoch(window);
		// waitOutEpoch(window);
		// waitDiffEpoch(window);
		// waitFenceEpoch(window);

		// window.free();

		// MPI.COMM_WORLD.Reduce(errors, 0, all_errors, 0, 1, MPI.INT, MPI.SUM, 0);
		MPI.Finalize();

		// if (rank[0] == 0 && all_errors[0] == 0)
			// System.out.printf("Test successful.\n");
			System.out.printf("Test skipped.\n");
	}

	// //PROC_NULL Communication
	// private static void procNullComm(Win window) throws MPIException {
	// 	Request pn_req[] = new Request[4];
	// 	int[] res = new int[1];
	// 	int[][] val = new int[4][1];

	// 	window.lockAll(0);

	// 	pn_req[0] = window.rGetAccumulate(val[0], 1, MPI.INT, res, 1, MPI.INT, MPI.PROC_NULL, 0, 1, MPI.INT, MPI.REPLACE);
	// 	pn_req[1] = window.rGet(val[1], 1, MPI.INT, MPI.PROC_NULL, 1, 1, MPI.INT);
	// 	pn_req[2] = window.rPut(val[2], 1, MPI.INT, MPI.PROC_NULL, 1, 1, MPI.INT);
	// 	pn_req[3] = window.rAccumulate(val[3], 1, MPI.INT, MPI.PROC_NULL, 0, 1, MPI.INT, MPI.REPLACE);

	// 	assert(pn_req[0] != MPI.REQUEST_NULL);
	// 	assert(pn_req[1] != MPI.REQUEST_NULL);
	// 	assert(pn_req[2] != MPI.REQUEST_NULL);
	// 	assert(pn_req[3] != MPI.REQUEST_NULL);

	// 	window.unlockAll();

	// 	Request.Waitall(pn_req);

	// 	MPI.COMM_WORLD.Barrier();

	// 	window.lock(MPI.LOCK_SHARED, 0, 0);
	// }

	// /* GET-ACC: Test third-party communication, through rank 0. */
	// private static void getACC(Win window, int[] rank, IntBuffer buf) throws MPIException {
	// 	for (int i = 0; i < ITER; i++) {
	// 		Request gacc_req;
	// 		int[] val = new int[1];
	// 		val[0] = -1;
	// 		int exp = -1;

	// 		/* Processes form a ring.  Process 0 starts first, then passes a token
	// 		 * to the right.  Each process, in turn, performs third-party
	// 		 * communication via process 0's window. */
	// 		if (rank[0] > 0) {
	// 			MPI.COMM_WORLD.Recv(null, 0, 0, MPI.BYTE, (rank[0] - 1), 0);
	// 		}

	// 		gacc_req = window.rGetAccumulate(rank, 1, MPI.INT, val, 1, MPI.INT, 0, 0, 1, MPI.INT, MPI.REPLACE);
	// 		assert(gacc_req != MPI.REQUEST_NULL);
	// 		gacc_req.Wait();

	// 		exp = (rank[0] + nproc - 1) % nproc;

	// 		if (val[0] != exp) {
	// 			System.out.println("getACC " + rank + " - Got " + val[0] + ", expected " + exp + "\n");
	// 			errors[0]++;
	// 		}

	// 		if (rank[0] < nproc - 1) {
	// 			MPI.COMM_WORLD.Send(null, 0, 0, MPI.BYTE, (rank[0] + 1), 0);
	// 		}
	// 		MPI.COMM_WORLD.Barrier();
	// 	}

	// 	MPI.COMM_WORLD.Barrier();

	// 	if (rank[0] == 0)
	// 		buf.put(0, (nproc - 1));

	// 	window.sync();
	// }

	// /* GET+PUT: Test third-party communication, through rank 0. */
	// private static void getAndPut(Win window, int[] rank, IntBuffer buf) throws MPIException {
	// 	for (int i = 0; i < ITER; i++) {
	// 		Request req;
	// 		int[] val = new int[1];
	// 		val[0] = -1;
	// 		int exp = -1;

	// 		/* Processes form a ring.  Process 0 starts first, then passes a token
	// 		 * to the right.  Each process, in turn, performs third-party
	// 		 * communication via process 0's window. */
	// 		if (rank[0] > 0) {
	// 			MPI.COMM_WORLD.Recv(null, 0, 0, MPI.BYTE, (rank[0] - 1), 0);
	// 		}

	// 		req = window.rGet(val, 1, MPI.INT, 0, 0, 1, MPI.INT);
	// 		assert(req != MPI.REQUEST_NULL);
	// 		req.Wait();

	// 		req = window.rPut(rank, 1, MPI.INT, 0, 0, 1, MPI.INT);
	// 		assert(req != MPI.REQUEST_NULL);
	// 		req.Wait();

	// 		exp = (rank[0] + nproc - 1) % nproc;

	// 		if (val[0] != exp) {
	// 			System.out.printf("GET+PUT: %d - Got %d, expected %d\n", rank[0], val[0], exp);
	// 			errors[0]++;
	// 		}

	// 		/* must wait for remote completion for the result to be correct for the next proc */

	// 		window.flush(0);

	// 		if (rank[0] < nproc - 1) {
	// 			MPI.COMM_WORLD.Send(null, 0, 0, MPI.BYTE, (rank[0] + 1), 0);
	// 		}
	// 		MPI.COMM_WORLD.Barrier();
	// 	}
	// 	MPI.COMM_WORLD.Barrier();

	// 	if (rank[0] == 0)
	// 		buf.put(0, (nproc - 1));

	// 	window.sync();
	// }

	// /* GET+ACC: Test third-party communication, through rank 0. */
	// private static void getAndACC(Win window, int[] rank) throws MPIException {
	// 	for (int i = 0; i < ITER; i++) {
	// 		Request req;
	// 		int[] val = new int[1];
	// 		val[0] = -1;
	// 		int exp = -1;

	// 		/* Processes form a ring.  Process 0 starts first, then passes a token
	// 		 * to the right.  Each process, in turn, performs third-party
	// 		 * communication via process 0's window. */
	// 		if (rank[0] > 0) {
	// 			MPI.COMM_WORLD.Recv(null, 0, 0, MPI.BYTE, (rank[0] - 1), 0);
	// 		}

	// 		req = window.rGet(val, 1, MPI.INT, 0, 0, 1, MPI.INT);
	// 		assert(req != MPI.REQUEST_NULL);
	// 		req.Wait();

	// 		req = window.rAccumulate(rank,  1, MPI.INT, 0, 0, 1, MPI.INT, MPI.REPLACE);
	// 		assert(req != MPI.REQUEST_NULL);
	// 		req.Wait();

	// 		exp = (rank[0] + nproc - 1) % nproc;

	// 		if (val[0] != exp) {
	// 			System.out.println("getAndACC " + rank + " - Got " + val[0] + ", expected " + exp + "\n");
	// 			errors[0]++;
	// 		}

	// 		/* must wait for remote completion for the result to be correct for the next proc */
	// 		window.flush(0);
	// 		if (rank[0] < nproc - 1) {
	// 			MPI.COMM_WORLD.Send(null, 0, 0, MPI.BYTE, (rank[0] + 1), 0);
	// 		}
	// 		MPI.COMM_WORLD.Barrier();
	// 	}
	// 	window.unlock(0);

	// 	MPI.COMM_WORLD.Barrier();
	// }

	// /* Wait inside of an epoch */
	// private static void waitInEpoch(Win window) throws MPIException {
	// 	Request[] pn_req = new Request[4];
	// 	int[][] val = new int[4][1];
	// 	int[] res = new int[1];
	// 	int target = 0;

	// 	window.lockAll(0);

	// 	pn_req[0] = window.rGetAccumulate(val[0], 1, MPI.INT, res, 1, MPI.INT, target, 0, 1, MPI.INT, MPI.REPLACE);
	// 	pn_req[1] = window.rGet(val[1], 1, MPI.INT, target, 1, 1, MPI.INT);
	// 	pn_req[2] = window.rPut(val[2], 1, MPI.INT, target, 2, 1, MPI.INT);
	// 	pn_req[3] = window.rAccumulate(val[3], 1, MPI.INT, target, 3, 1, MPI.INT, MPI.REPLACE);

	// 	assert(pn_req[0] != MPI.REQUEST_NULL);
	// 	assert(pn_req[1] != MPI.REQUEST_NULL);
	// 	assert(pn_req[2] != MPI.REQUEST_NULL);
	// 	assert(pn_req[3] != MPI.REQUEST_NULL);

	// 	Request.Waitall(pn_req);

	// 	window.unlockAll();

	// 	MPI.COMM_WORLD.Barrier();
	// }

	// /* Wait outside of an epoch */
	// private static void waitOutEpoch(Win window) throws MPIException {
	// 	Request[] pn_req = new Request[4];
	// 	int[][] val = new int[4][1];
	// 	int[] res = new int[1];
	// 	int target = 0;

	// 	window.lockAll(0);

	// 	pn_req[0] = window.rGetAccumulate(val[0], 1, MPI.INT, res, 1, MPI.INT, target, 0, 1, MPI.INT, MPI.REPLACE);
	// 	pn_req[1] = window.rGet(val[1], 1, MPI.INT, target, 1, 1, MPI.INT);
	// 	pn_req[2] = window.rPut(val[2], 1, MPI.INT, target, 2, 1, MPI.INT);
	// 	pn_req[3] = window.rAccumulate(val[3], 1, MPI.INT, target, 3, 1, MPI.INT, MPI.REPLACE);

	// 	assert(pn_req[0] != MPI.REQUEST_NULL);
	// 	assert(pn_req[1] != MPI.REQUEST_NULL);
	// 	assert(pn_req[2] != MPI.REQUEST_NULL);
	// 	assert(pn_req[3] != MPI.REQUEST_NULL);

	// 	window.unlockAll();

	// 	Request.Waitall(pn_req);
	// }

	// /* Wait in a different epoch */
	// private static void waitDiffEpoch(Win window) throws MPIException {
	// 	Request[] pn_req = new Request[4];
	// 	int[][] val = new int[4][1];
	// 	int[] res = new int[1];
	// 	int target = 0;

	// 	window.lockAll(0);

	// 	pn_req[0] = window.rGetAccumulate(val[0], 1, MPI.INT, res, 1, MPI.INT, target, 0, 1, MPI.INT, MPI.REPLACE);
	// 	pn_req[1] = window.rGet(val[1], 1, MPI.INT, target, 1, 1, MPI.INT);
	// 	pn_req[2] = window.rPut(val[2], 1, MPI.INT, target, 2, 1, MPI.INT);
	// 	pn_req[3] = window.rAccumulate(val[3], 1, MPI.INT, target, 3, 1, MPI.INT, MPI.REPLACE);

	// 	assert(pn_req[0] != MPI.REQUEST_NULL);
	// 	assert(pn_req[1] != MPI.REQUEST_NULL);
	// 	assert(pn_req[2] != MPI.REQUEST_NULL);
	// 	assert(pn_req[3] != MPI.REQUEST_NULL);

	// 	window.unlockAll();

	// 	window.lockAll(0);
	// 	Request.Waitall(pn_req);
	// 	window.unlockAll();
	// }

	// /* Wait in a fence epoch */
	// private static void waitFenceEpoch(Win window) throws MPIException {
	// 	Request[] pn_req = new Request[4];
	// 	int[][] val = new int[4][1];
	// 	int[] res = new int[1];
	// 	int target = 0;

	// 	window.lockAll(0);

	// 	pn_req[0] = window.rGetAccumulate(val[0], 1, MPI.INT, res, 1, MPI.INT, target, 0, 1, MPI.INT, MPI.REPLACE);
	// 	pn_req[1] = window.rGet(val[1], 1, MPI.INT, target, 1, 1, MPI.INT);
	// 	pn_req[2] = window.rPut(val[2], 1, MPI.INT, target, 2, 1, MPI.INT);
	// 	pn_req[3] = window.rAccumulate(val[3], 1, MPI.INT, target, 3, 1, MPI.INT, MPI.REPLACE);

	// 	assert(pn_req[0] != MPI.REQUEST_NULL);
	// 	assert(pn_req[1] != MPI.REQUEST_NULL);
	// 	assert(pn_req[2] != MPI.REQUEST_NULL);
	// 	assert(pn_req[3] != MPI.REQUEST_NULL);

	// 	window.unlockAll();

	// 	window.fence(0);
	// 	Request.Waitall(pn_req);
	// 	window.fence(0);
	// }
}
