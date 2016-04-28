package microbenchmarks.onesided;

import mpi.Group;
import mpi.MPI;

public class CasLatencyTest extends OnesidedTestBase {
	private int i;
	int disp;
	double tStart;
	double tEnd;
	private mpi.Win win;
	private long[] sbuf = new long[MAX_SIZE];
	private long[] cbuf = new long[MAX_SIZE];
	private long[] tbuf = new long[MAX_SIZE];

	@Override
	protected void initialize(String[] args) {
		super.initialize(args);
		getReporter().setColumnNames(new String[] { "Latency (us)" });
	}
	
	private double collectResult(double tStart, double tEnd) {
		return collectResult(tStart, tEnd, 1.0);
	}

	private double collectResult(double tStart, double tEnd, double multiplier) {
		double lat = (tEnd - tStart) * (10 ^ 6) / getLoop() * multiplier;
		if (getRank() == 0)
			getReporter().collect(new Object[] { new Double(lat) });
		return lat;
	}

	@Override
	protected void runTestWithFlush() {
		win = allocateMemory(MAX_SIZE);

		if (getRank() == 0) {
			win.lock(MPI.LOCK_SHARED, 1, 0);

			for (i = 0; i < getSkip() + getLoop(); i++) {
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
				win.flush(1);
			}

			tEnd = MPI.Wtime();
			win.unlock(1);
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd);

		win.free();
	}

	@Override
	protected void runTestWithFlushLocal() {
		win = allocateMemory(MAX_SIZE);

		if (getRank() == 0) {
			win.lock(MPI.LOCK_SHARED, 1, 0);

			for (i = 0; i < getSkip() + getLoop(); i++) {
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
				win.flushLocal(1);
			}
			tEnd = MPI.Wtime();
			win.unlock(1);
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd);

		win.free();
	}

	@Override
	protected void runTestWithLockAll() {
		win = allocateMemory(MAX_SIZE);

		if (getRank() == 0) {
			for (i = 0; i < getSkip() + getLoop(); i++) {
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.lockAll(0);
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
				win.unlockAll();
			}
			tEnd = MPI.Wtime();
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd);

		win.free();
	}

	@Override
	protected void runTestWithFence() {
		win = allocateMemory(MAX_SIZE);
		MPI.COMM_WORLD.Barrier();

		if (getRank() == 0) {
			for (i = 0; i < getSkip() + getLoop(); i++) {
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.fence(0);
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
				win.fence(0);
				win.fence(0);
			}
			tEnd = MPI.Wtime();
		} else {
			for (i = 0; i < getSkip() + getLoop(); i++) {
				win.fence(0);
				win.fence(0);
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 0, 0);
				win.fence(0);
			}
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd, 0.5);

		win.free();
	}

	@Override
	protected void runTestWithPscw() {
		int[] destrank = new int[1];
		Group group, commGroup;

		commGroup = MPI.COMM_WORLD.Group();

		win = allocateMemory(MAX_SIZE);

		if (getRank() == 0) {
			destrank[0] = 1;
			group = commGroup.Incl(destrank);
			MPI.COMM_WORLD.Barrier();

			for (i = 0; i < getSkip() + getLoop(); i++) {
				win.start(group, 0);
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
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
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 0, 0);
				win.complete();
			}
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd, 0.5);

		group.free();

		win.free();

		commGroup.free();
	}

	@Override
	protected void runTestWithLock() {
		win = allocateMemory(MAX_SIZE);

		if (getRank() == 0) {
			for (i = 0; i < getSkip() + getLoop(); i++) {
				if (i == getSkip()) {
					tStart = MPI.Wtime();
				}
				win.lock(MPI.LOCK_SHARED, 1, 0);
				win.compareAndSwap(sbuf, cbuf, tbuf, MPI.LONG, 1, 0);
				win.unlock(1);
			}
			tEnd = MPI.Wtime();
		}

		MPI.COMM_WORLD.Barrier();

		collectResult(tStart, tEnd);

		win.free();
	}

}
