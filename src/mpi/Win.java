package mpi;

import java.nio.ByteBuffer;

public class Win {

	// Window flavor constants
	public static final int FLAVOR_CREATE 	= 0;
	public static final int FLAVOR_ALLOCATE = 1;
	public static final int FLAVOR_DYNAMIC 	= 2;
	public static final int FLAVOR_SHARED 	= 3;

	// Window memory model constants
	public static final int SEPARATE = 0;
	public static final int UNIFIED  = 1;

	// MPJ Device Window which this class wraps
	private mpjdev.Win mpjDevWin;

	private Win(mpjdev.Win mpjDevWin) {
		this.mpjDevWin = mpjDevWin;
	}

	public static mpi.Win create(java.nio.ByteBuffer base, int disp_unit, mpi.Comm comm) {
		return new mpi.Win(mpjdev.Win.create(base, disp_unit, comm.mpjdevComm));
	}

	public static mpi.Win allocate(int size, int disp_unit, mpi.Comm comm) {
		return new mpi.Win(mpjdev.Win.allocate(size, disp_unit, comm.mpjdevComm));

	}

	public static mpi.Win allocateShared(int size, int disp_unit, mpi.Comm comm) {
		return new mpi.Win(mpjdev.Win.allocateShared(size, disp_unit, comm.mpjdevComm));

	}

	public static mpi.Win createDynamic(mpi.Comm comm) {
		return new mpi.Win(mpjdev.Win.createDynamic(comm.mpjdevComm));
	}

	public void attach(ByteBuffer base, int size) {
		mpjDevWin.attach(base, size);
	}

	public void detach(ByteBuffer base) {
		mpjDevWin.detach(base);
	}

	public void free() {
		mpjDevWin.free();
		mpjDevWin = null;
	}

	public ByteBuffer getBase() {
		return mpjDevWin.getBase();
	}

	public int getSize() {
		return mpjDevWin.getSize();
	}

	public int getDispUnit() {
		return mpjDevWin.getDispUnit();
	}

	public int getCreateFlavor() {
		return mpjDevWin.getCreateFlavor();
	}

	public int getModel() {
		return mpjDevWin.getModel();
	}

	public void put(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		mpjDevWin.put(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype);
	}

	public void get(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		mpjDevWin.get(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype);
	}

	public void accumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		mpjDevWin.accumulate(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, op);
	}

	public void getAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		mpjDevWin.getAccumulate(origin_buffer, origin_count, origin_datatype, result_buffer, result_count, result_datatype, target_rank, target_disp, target_count, target_datatype, op);
	}

	public void fetchAndOp(Object origin_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp, mpi.Op op) {
		mpjDevWin.fetchAndOp(origin_buffer, result_buffer, datatype, target_rank, target_disp, op);
	}

	public void compareAndSwap(Object origin_buffer, Object compare_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp) {
		mpjDevWin.compareAndSwap(origin_buffer, compare_buffer, result_buffer, datatype, target_rank, target_disp);
	}

	public mpi.Request rPut(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		return new mpi.Request(mpjDevWin.rPut(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype));
	}

	public mpi.Request rGet(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		return new mpi.Request(mpjDevWin.rGet(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype));
	}

	public mpi.Request rAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		return new mpi.Request(mpjDevWin.rAccumulate(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, op));
	}

	public mpi.Request rGetAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		return new mpi.Request(mpjDevWin.rGetAccumulate(origin_buffer, origin_count, origin_datatype, result_buffer, result_count, result_datatype, target_rank, target_disp, target_count, target_datatype, op));
	}

	public void fence(int assertions) {
		mpjDevWin.fence(assertions);
	}

	public void start(mpi.Group group, int assertions) {
		mpjDevWin.start(group, assertions);
	}

	public void complete() {
		mpjDevWin.complete();
	}

	public void post(mpi.Group group, int assertions) {
		mpjDevWin.post(group, assertions);
	}

	public void waitFor() {
		mpjDevWin.waitFor();
	}

	public boolean test() {
		return mpjDevWin.test();
	}

	public void lock(int lock_type, int rank, int assertions) {
		if (lock_type != MPI.LOCK_SHARED && lock_type != MPI.LOCK_EXCLUSIVE)
			throw new java.lang.IllegalArgumentException("Only MPI.LOCK_SHARED and MPI.LOCK_EXCLUSIVE lock types are supported.");
		mpjDevWin.lock(lock_type, rank, assertions);
	}

	public void lockAll(int assertions) {
		mpjDevWin.lockAll(assertions);
	}

	public void unlock(int rank) {
		mpjDevWin.unlock(rank);
	}

	public void unlockAll() {
		mpjDevWin.unlockAll();
	}

	public void flush(int rank) {
		mpjDevWin.flush(rank);
	}

	public void flushAll() {
		mpjDevWin.flushAll();
	}

	public void flushLocal(int rank) {
		mpjDevWin.flushLocal(rank);
	}

	public void flushLocalAll() {
		mpjDevWin.flushLocalAll();
	}

	public void sync() {
		mpjDevWin.sync();
	}

}