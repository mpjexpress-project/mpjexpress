package microbenchmarks.onesided;

import mpi.*;

public class PutBiBandwidthTest extends OnesidedTestBase {
	private int size, i, j, disp;
	private double tStart, tEnd;
	private Win win;
	private byte[] sbuf = new byte[MAX_SIZE];

	@Override
	protected void initialize(String[] args) {
		super.initialize(args);
		getReporter().setColumnNames(new String[] { "Size (byte)", "Bandwidth (MB/s)" });
	}

	private double collectResult(int size, double tStart, double tEnd) {
		double bw = (size / (10 ^ 6) * getLoop() * WINDOW_SIZE_LARGE) / (tEnd - tStart) * 2;
		if (getRank() == 0)
			getReporter().collect(new Object[] { new Integer(size), new Double(bw) });
		return bw;
	}

	@Override
	protected void runTestWithFlush() {
		throw new UnsupportedOperationException("Flush sync type is not supported in Put Bidirectional BW test");
	}

	@Override
	protected void runTestWithFlushLocal() {
		throw new UnsupportedOperationException("Flush-local sync type is not supported in Put Bidirectional BW test");
	}

	@Override
	protected void runTestWithLockAll() {
		throw new UnsupportedOperationException("Lock-all sync type is not supported in Put Bidirectional BW test");
	}
	
	@Override
	protected void runTestWithLock() {
		throw new UnsupportedOperationException("Lock sync type is not supported in Put Bidirectional BW test");
	}

	@Override
	protected void runTestWithFence() {
		int window_size = WINDOW_SIZE_LARGE;

		for (size = 1; size <= MAX_SIZE; size = size * 2) {
			win = allocateMemory(size * window_size);
			MPI.COMM_WORLD.Barrier();

			if (getRank() == 0) {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.fence(0);
					for (j = 0; j < window_size; j++) {
						// TODO Slice sbuf
						win.put(sbuf, size, MPI.BYTE, 1, disp + (j * size), size, MPI.BYTE);
					}
					win.fence(0);
				}
				tEnd = MPI.Wtime();
			} else {
				for (i = 0; i < getSkip() + getLoop(); i++) {
					win.fence(0);
					for (j = 0; j < window_size; j++) {
						// TODO Slice sbuf
						win.put(sbuf, size, MPI.BYTE, 0, disp + (j * size), size, MPI.BYTE);
					}
					win.fence(0);
				}
			}

			MPI.COMM_WORLD.Barrier();

			collectResult(size, tStart, tEnd);

			win.free();
		}
	}

	@Override
	protected void runTestWithPscw() {
		int window_size = WINDOW_SIZE_LARGE;
		int[] destrank = new int[1];
		Group commGroup, group;
		
		commGroup = MPI.COMM_WORLD.Group();

		for (size = 1; size <= MAX_SIZE; size = size * 2) {
			win = allocateMemory(size * window_size);
			MPI.COMM_WORLD.Barrier();

			if (getRank() == 0) {
				destrank[0] = 1;
				group = commGroup.Incl(destrank);
				
				for (i = 0; i < getSkip() + getLoop(); i++) {
					if (i == getSkip()) {
						tStart = MPI.Wtime();
					}
					win.post(group, 0);
					win.start(group, 0);
					for (j = 0; j < window_size; j++) {
						// TODO Slice sbuf
						win.put(sbuf, size, MPI.BYTE, 1, disp + (j * size), size, MPI.BYTE);
					}
					win.complete();
					win.waitFor();
				}
				tEnd = MPI.Wtime();
			} else {
				destrank[0] = 0;
				group = commGroup.Incl(destrank);
				for (i = 0; i < getSkip() + getLoop(); i++) {
					win.post(group, 0);
					win.start(group, 0);
					for (j = 0; j < window_size; j++) {
						// TODO Slice sbuf
						win.put(sbuf, size, MPI.BYTE, 0, disp + (j * size), size, MPI.BYTE);
					}
					win.complete();
					win.waitFor();
				}
			}

			MPI.COMM_WORLD.Barrier();
			group.free();

			collectResult(size, tStart, tEnd);

			win.free();
		}
		
		commGroup.free();
	}

}
