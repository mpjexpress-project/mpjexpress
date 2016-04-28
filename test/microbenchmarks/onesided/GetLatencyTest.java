package microbenchmarks.onesided;

import mpi.Group;
import mpi.MPI;

public class GetLatencyTest extends OnesidedTestBase {
	private int size, i;
	int disp;
	double tStart;
	double tEnd;
	private mpi.Win win;
	private byte[] rbuf = new byte[MAX_SIZE];
	
	@Override
	protected void initialize(String[] args) {
		super.initialize(args);
		getReporter().setColumnNames(new String[] { "Size (bytes)", "Latency (us)" });
	}
	
	private double collectResult(int size, double tStart, double tEnd) {
		return collectResult(size, tStart, tEnd, 1.0);
	}
	
	private double collectResult(int size, double tStart, double tEnd, double multiplier) {
		double lat = (tEnd-tStart) * (10^6) / getLoop() * multiplier;
		if (getRank() == 0)
			getReporter().collect(new Object[] { new Integer(size), new Double(lat) });
		return lat;
	}

	@Override
	protected void runTestWithFlush() {
		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);

			if (getRank() == 0) {
				win.lock(MPI.LOCK_SHARED, 1, 0);

				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.flush(1);
				}

				tEnd = MPI.Wtime();
				win.unlock(1);
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd);

			win.free();
		}
	}

	@Override
	protected void runTestWithFlushLocal() {
		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);

			if (getRank() == 0) {
				win.lock(MPI.LOCK_SHARED, 1, 0);

				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.flushLocal(1);
				}
				tEnd = MPI.Wtime();
				win.unlock(1);
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd);

			win.free();
		}
	}

	@Override
	protected void runTestWithLockAll() {
		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);

			if (getRank() == 0) {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.lockAll(0);
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.unlockAll();
				}
				tEnd = MPI.Wtime();
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd);

			win.free();
		}
	}

	@Override
	protected void runTestWithFence() {
		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);
			MPI.COMM_WORLD.Barrier();

			if (getRank() == 0) {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.fence(0);
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.fence(0);
					win.fence(0);
				}
				tEnd = MPI.Wtime();
			} else {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					win.fence(0);
					win.fence(0);
					win.get(rbuf, size, MPI.BYTE, 0, disp, size, MPI.BYTE);
					win.fence(0);
				}
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd, 0.5);

			win.free();
		}
	}

	@Override
	protected void runTestWithPscw() {
		int[] destrank = new int[1];
		Group group, commGroup;

		commGroup = MPI.COMM_WORLD.Group();

		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);

			if (getRank() == 0) {
				destrank[0] = 1;
				group = commGroup.Incl(destrank);
				MPI.COMM_WORLD.Barrier();

				for (i = 0; i < getSkip() + getLoop(); i++) {
					win.start(group, 0);
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.complete();
					win.post(group, 0);
					win.waitFor();
				}
				tEnd = MPI.Wtime();
			} else {
				destrank[0] = 0;
				group = commGroup.Incl(destrank);
				MPI.COMM_WORLD.Barrier();

				for (i = 0; i < getSkip() + getLoop(); i++) {
					win.post(group, 0);
					win.waitFor();
					win.start(group, 0);
					win.get(rbuf, size, MPI.BYTE, 0, disp, size, MPI.BYTE);
					win.complete();
				}
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd, 0.5);

			group.free();

			win.free();
		}
		
		commGroup.free();
	}

	@Override
	protected void runTestWithLock() {
		for (size = 0; size <= MAX_SIZE; size = ((size != 0) ? size * 2 : 1)) {
			win = allocateMemory(size);

			if (getRank() == 0) {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.lock(MPI.LOCK_SHARED, 1, 0);
					win.get(rbuf, size, MPI.BYTE, 1, disp, size, MPI.BYTE);
					win.unlock(1);
				}
				tEnd = MPI.Wtime();
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd);

			win.free();
		}
	}

}
