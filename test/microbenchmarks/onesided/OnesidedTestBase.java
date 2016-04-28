package microbenchmarks.onesided;

import java.nio.ByteBuffer;

import microbenchmarks.TestBase;

import mpi.MPI;
import mpi.Win;

public abstract class OnesidedTestBase extends TestBase {

	protected static final int MAX_SIZE = 1 << 22;
	protected static final int WINDOW_SIZE_LARGE = 32;
	protected static final int LARGE_MESSAGE_SIZE = 8192;
	private int rank;
	private int nprocs;
	private int loop;
	private int skip;
	private String syncType;
	private String winType;

	public OnesidedTestBase() {
		super();
	}

	@Override
	protected void initialize(String[] args) {
		MPI.Init(args);
		setRank(MPI.COMM_WORLD.Rank());
		setNprocs(MPI.COMM_WORLD.Size());
		
		setLoop(Integer.parseInt(getProperties().getProperty("Loop")));
		setSkip(Integer.parseInt(getProperties().getProperty("Skip")));
		setSyncType(getProperties().getProperty("SyncType").toLowerCase());
		setWinType(getProperties().getProperty("WinType").toLowerCase());

		if (getRank() == 0) {
			System.out.println("-- listing Test config --");
			System.out.println("TestName=" + this.getClass().getSimpleName());
			System.out.println("NProcs=" + getNprocs());
			getProperties().list(System.out);
			System.out.println("-- running test --");
		}
	}

	@Override
	protected void cleanUp() {
		MPI.Finalize();
	}

	protected int getRank() {
		return rank;
	}

	protected void setRank(int rank) {
		this.rank = rank;
	}

	protected int getNprocs() {
		return nprocs;
	}

	protected void setNprocs(int nprocs) {
		this.nprocs = nprocs;
	}

	protected int getLoop() {
		return loop;
	}

	protected void setLoop(int loop) {
		this.loop = loop;
	}

	protected int getSkip() {
		return skip;
	}

	protected void setSkip(int skip) {
		this.skip = skip;
	}

	protected String getSyncType() {
		return syncType;
	}

	protected void setSyncType(String syncType) {
		this.syncType = syncType;
	}

	protected String getWinType() {
		return winType;
	}

	protected void setWinType(String winType) {
		this.winType = winType;
	}

	protected mpi.Win allocateMemory(int size) {
		mpi.Win win;
		switch (getWinType()) {
		case "create":
			win = Win.create(ByteBuffer.allocateDirect(size), 1, MPI.COMM_WORLD);
			break;
		case "allocate":
			win = Win.allocate(size, 1, MPI.COMM_WORLD);
			break;
		case "dynamic":
			win = Win.createDynamic(MPI.COMM_WORLD);
			win.attach(ByteBuffer.allocateDirect(size), size);
			break;
		default:
			throw new IllegalArgumentException(
					"Invalid property value for WinType." + "Valid values are create, allocate and dynamic.");
		}
		return win;
	}

	@Override
	public void runTest() {
		switch (getSyncType()) {
		case "lock":
			runTestWithLock();
			break;
		case "pscw":
			runTestWithPscw();
			break;
		case "fence":
			runTestWithFence();
			break;
		case "lockall":
			runTestWithLockAll();
			break;
		case "flushlocal":
			runTestWithFlushLocal();
			break;
		case "flush":
			runTestWithFlush();
			break;
		default:
			throw new IllegalArgumentException("Invalid property value for SyncType.\n"
					+ "Valid values are lock, pscw, fence, lockall, flush and flushlocal.");
		}
	}

	protected abstract void runTestWithFlush();

	protected abstract void runTestWithFlushLocal();

	protected abstract void runTestWithLockAll();

	protected abstract void runTestWithFence();

	protected abstract void runTestWithPscw();

	protected abstract void runTestWithLock();

}