package mpjdev;

import java.nio.ByteBuffer;

public abstract class Win {

	public static mpjdev.Win create(java.nio.ByteBuffer base, int disp_unit, mpjdev.Comm comm) {
		if (mpjdev.Constants.isNative)
			return mpjdev.natmpjdev.Win.create(base, disp_unit, comm);
		else
			return null;
		// return mpjdev.javampjdev.Win.create(base, disp_unit, comm);
	}

	public static mpjdev.Win allocate(int size, int disp_unit, mpjdev.Comm comm) {
		if (mpjdev.Constants.isNative)
			return mpjdev.natmpjdev.Win.allocate(size, disp_unit, comm);
		else
			return null;
		// return mpjdev.javampjdev.Win.allocate(size, disp_unit, comm);
	}

	public static mpjdev.Win allocateShared(int size, int disp_unit, mpjdev.Comm comm) {
		if (mpjdev.Constants.isNative)
			return mpjdev.natmpjdev.Win.allocateShared(size, disp_unit, comm);
		else
			return null;
		// return mpjdev.javampjdev.Win.allocateShared(size, disp_unit, comm);
	}

	public static mpjdev.Win createDynamic(mpjdev.Comm comm) {
		if (mpjdev.Constants.isNative)
			return mpjdev.natmpjdev.Win.createDynamic(comm);
		else
			return null;
		// return mpjdev.javampjdev.Win.createDynamic(comm);
	}

	public abstract void attach(ByteBuffer base, int size);

	public abstract void detach(ByteBuffer base);

	public abstract void free();

	public abstract ByteBuffer getBase();

	public abstract int getSize();

	public abstract int getDispUnit();

	public abstract int getCreateFlavor();

	public abstract int getModel();

	public abstract void put(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype);

	public abstract void get(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype);

	public abstract void accumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op);

	public abstract void getAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op);

	public abstract void fetchAndOp(Object origin_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp, mpi.Op op);

	public abstract void compareAndSwap(Object origin_buffer, Object compare_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp);

	public abstract mpjdev.Request rPut(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype);

	public abstract mpjdev.Request rGet(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype);

	public abstract mpjdev.Request rAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op);

	public abstract mpjdev.Request rGetAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op);

	public abstract void fence(int assertions);

	public abstract void start(mpi.Group group, int assertions);

	public abstract void complete();

	public abstract void post(mpi.Group group, int assertions);

	public abstract void waitFor();

	public abstract boolean test();

	public abstract void lock(int lock_type, int rank, int assertions);

	public abstract void lockAll(int assertions);

	public abstract void unlock(int rank);

	public abstract void unlockAll();

	public abstract void flush(int rank);

	public abstract void flushAll();

	public abstract void flushLocal(int rank);

	public abstract void flushLocalAll();

	public abstract void sync();

}