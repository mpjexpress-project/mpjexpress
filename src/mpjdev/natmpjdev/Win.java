package mpjdev.natmpjdev;

import java.nio.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import mpjdev.natmpjdev.util.Allocators;
import mpjdev.natmpjdev.util.Allocator;

public class Win extends mpjdev.Win {

	/*
	 * Copy pasted these things (ENUMs for Types) from mpi/dataType.java These
	 * have to be consistent with mpi/dataType.java some how have to expose them
	 * in dataType so that they be accessible to other classes
	 */
	final static int UNDEFINED = -1;
	final static int NULL = 0;
	final static int BYTE = 1;
	final static int CHAR = 2;
	final static int SHORT = 3;
	final static int BOOLEAN = 4;
	final static int INT = 5;
	final static int LONG = 6;
	final static int FLOAT = 7;
	final static int DOUBLE = 8;
	final static int PACKED = 9;
	// after this non primitive types start
	final static int PRIMITIVE_TYPE_RANGE_UB = 9;

	final static int LB = 10;
	final static int UB = 11;
	final static int OBJECT = 12;

	final static int SHORT2 = 3;
	final static int INT2 = 5;
	final static int LONG2 = 6;
	final static int FLOAT2 = 7;
	final static int DOUBLE2 = 8;

	private static final Allocator bufferAllocator = Allocators.getNewAllocator();


	public static mpjdev.Win create(java.nio.ByteBuffer base, int disp_unit, mpjdev.Comm comm) {
		if (base.isDirect()) {
			long winptr = nativeCreateWin(base, base.capacity(), disp_unit, ((mpjdev.natmpjdev.Comm)comm).handle);
			return new Win(winptr, (mpjdev.natmpjdev.Comm)comm);
		} else {
			throw new IllegalArgumentException("Cannot create window from non-direct ByteBuffer.");
		}
	}

	private static native long nativeCreateWin(java.nio.ByteBuffer base, int size, int disp_unit, long comm);

	public static mpjdev.Win allocate(int size, int disp_unit, mpjdev.Comm comm) {
		long winptr = nativeAllocWin(size, disp_unit, ((mpjdev.natmpjdev.Comm)comm).handle);
		return new Win(winptr, (mpjdev.natmpjdev.Comm)comm);
	}

	public static native long nativeAllocWin(int size, int disp_unit, long comm);

	public static mpjdev.Win allocateShared(int size, int disp_unit, mpjdev.Comm comm) {
		throw new UnsupportedOperationException("allocShared not available in current release.");
	}

	public static mpjdev.Win createDynamic(mpjdev.Comm comm) {
		long winptr = nativeCreateDynamicWin(((mpjdev.natmpjdev.Comm)comm).handle);
		return new Win(winptr, (mpjdev.natmpjdev.Comm)comm);
	}

	private static native long nativeCreateDynamicWin(long comm);


	// Pointer to native C structure
	private long nativeHandle;

	// Native MPJ Device Comm
	mpjdev.natmpjdev.Comm mpjdevNativeComm = null;

	// Sync manager keeps tracks of byteBuffers that need to be copied back
	// to the user buffers on synchronization calls and performs the copy.
	private LocalSyncManager syncManager = null;

	private Win(long handle, mpjdev.natmpjdev.Comm comm) {
		this.nativeHandle = handle;
		this.mpjdevNativeComm = comm;
		this.syncManager = new LocalSyncManager();
	}

	private long getHandle() {
		if (nativeHandle == 0)
			throw new IllegalStateException("Window can not be used after it is freed.");
		else
			return nativeHandle;
	}

	public void attach(ByteBuffer base, int size) {
		nativeAttachWin(base, size, this.getHandle());
	}

	private native void nativeAttachWin(ByteBuffer base, int size, long win);

	public void detach(ByteBuffer base) {
		nativeDetachWin(base, this.getHandle());
	}

	private native void nativeDetachWin(ByteBuffer base, long win);

	public void free() {
		nativeFreeWin(this.getHandle());
		syncManager.DoSync();
		nativeHandle = 0;
		mpjdevNativeComm = null;
		syncManager = null;
	}

	private native void nativeFreeWin(long win);

	public ByteBuffer getBase() {
		return nativeGetBase(this.getHandle());
	}

	private native ByteBuffer nativeGetBase(long win);

	public int getSize() {
		return nativeGetSize(this.getHandle());
	}

	private native int nativeGetSize(long win);

	public int getDispUnit() {
		return nativeGetDispUnit(this.getHandle());
	}

	private native int nativeGetDispUnit(long win);

	public int getCreateFlavor() {
		return nativeGetCreateFlavor(this.getHandle());
	}

	private native int nativeGetCreateFlavor(long win);

	public int getModel() {
		return nativeGetModel(this.getHandle());
	}

	private native int nativeGetModel(long win);

	public void put(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		// 	super.Put(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, win);
		// 	return;
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			// I am not sure how the target displacement shud be calculated here.
			// We're providing MPI_BYTE as the data type for the target but the
			// actualy displacement in bytes is calculated based on what the target
			// process provided as disp_unit during window creation. Since MPI is doing
			// this, I have commented out my line.
			nativePut(wBuffer, numBytes, target_rank, target_disp, numBytes, this.getHandle());
			// nativeIntracomm.Put(wBuffer, numBytes, target_rank, target_disp*origin_datatype.getByteSize(), numBytes, win);

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativePut(java.nio.ByteBuffer origin_buffer, int origin_count, int target_rank, int target_disp, int target_count, long win);

	public void get(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		// 	super.Get(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, win);
		// 	return;
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			// byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			// wBuffer.flip();
			// wBuffer.limit(numBytes);

			// I am not sure how the target displacement shud be calculated here.
			// We're providing MPI_BYTE as the data type for the target but the
			// actualy displacement in bytes is calculated based on what the target
			// process provided as disp_unit during window creation. Since MPI is doing
			// this, I have commented out my line.
			nativeGet(wBuffer, numBytes, target_rank, target_disp, numBytes, this.getHandle());

			// Sync wbuffer back to origin_buffer when windows is syncrhonized
			syncManager.AddSync(wBuffer, origin_buffer, 0, 0, origin_count, origin_datatype.getType(), target_rank);

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativeGet(java.nio.ByteBuffer origin_buffer, int origin_count, int target_rank, int target_disp, int target_count, long win);

	public void accumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		// 	super.Accumulate(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, op, win);
		// 	return;
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			// I am not sure how the target displacement shud be calculated here.
			// We're providing MPI_BYTE as the data type for the target but the
			// actualy displacement in bytes is calculated based on what the target
			// process provided as disp_unit during window creation. Since MPI is doing
			// this, I have commented out my line.
			// nativeIntracomm.Accumulate(wBuffer, numBytes, target_rank, target_disp, numBytes, op, win);
			nativeAccumulate(wBuffer, origin_count, origin_datatype.baseType, target_rank, target_disp, target_count, target_datatype.baseType, op.opCode, this.getHandle());

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativeAccumulate(java.nio.ByteBuffer origin_buffer, int origin_count, int origin_datatype, int target_rank, int target_disp, int target_count, int target_datatype, int op, long win);

	public void getAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		//   super.Get_accumulate(origin_buffer, origin_count, origin_datatype, result_buffer, result_count, result_datatype, target_rank, target_disp, target_count, target_datatype, op, win);
		//   return;
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);
		ByteBuffer wResult = bufferAllocator.allocate(target_count * target_datatype.getByteSize());

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			nativeGetAccumulate(wBuffer, origin_count, origin_datatype.baseType, wResult, result_count, result_datatype.baseType, target_rank, target_disp, target_count, target_datatype.baseType, op.opCode, this.getHandle());

			// Sync wbuffer back to origin_buffer when windows is syncrhonized
			syncManager.AddSync(wResult, result_buffer, 0, 0, result_count, result_datatype.getType(), target_rank);

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativeGetAccumulate(java.nio.ByteBuffer origin_buffer, int origin_count, int origin_datatype, java.nio.ByteBuffer result_buffer, int result_count, int result_datatype, int target_rank, int target_disp, int target_count, int target_datatype, int op, long win);

	public void fetchAndOp(Object origin_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp, mpi.Op op) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || datatype.Size() > 1) {
		//   super.Fetch_and_op(origin_buffer, result_buffer, datatype, target_rank, target_disp, op, win);
		//   return;
		// }

		int numBytes = datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);
		ByteBuffer wResult = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, 1, datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			nativeFetchAndOp(wBuffer, wResult, datatype.baseType, target_rank, target_disp, op.opCode, this.getHandle());

			syncManager.AddSync(wResult, result_buffer, 0, 0, 1, datatype.getType(), target_rank);

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativeFetchAndOp(ByteBuffer origin_buffer, ByteBuffer result_buffer, int datatype, int target_rank, int target_disp, int op, long win);

	public void compareAndSwap(Object origin_buffer, Object compare_buffer, Object result_buffer, mpi.Datatype datatype, int target_rank, int target_disp) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || datatype.Size() > 1) {
		//   super.Compare_and_swap(origin_buffer, compare_buffer, result_buffer, datatype, target_rank, target_disp, win);
		//   return;
		// }

		int numBytes = datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);
		ByteBuffer wCompare = bufferAllocator.allocate(numBytes);
		ByteBuffer wResult = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, 1, datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);
			byteBufferSetData(compare_buffer, wCompare, 0, 0, 1, datatype.getType());
			wCompare.flip();
			wCompare.limit(numBytes);

			nativeCompareAndSwap(wBuffer, wCompare, wResult, datatype.baseType, target_rank, target_disp, this.getHandle());

			syncManager.AddSync(wResult, result_buffer, 0, 0, 1, datatype.getType(), target_rank);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private native void nativeCompareAndSwap(ByteBuffer origin_buffer, ByteBuffer compare_buffer, ByteBuffer result_buffer, int datatype, int target_rank, int target_disp, long win);

	public mpjdev.Request rPut(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		//   return super.Rput(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, win);
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			NativeSendRequest req = new NativeSendRequest();
			req.handle = nativeRput(wBuffer, numBytes, target_rank, target_disp, numBytes, this.getHandle());
			return req;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private native long nativeRput(java.nio.ByteBuffer origin_buffer, int origin_count, int target_rank, int target_disp, int target_count, long win);

	public mpjdev.Request rGet(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		//   return super.Rget(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, win);
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			// byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			// wBuffer.flip();
			// wBuffer.limit(numBytes);

			// I am not sure how the target displacement shud be calculated here.
			// We're providing MPI_BYTE as the data type for the target but the
			// actualy displacement in bytes is calculated based on what the target
			// process provided as disp_unit during window creation. Since MPI is doing
			// this, I have commented out my line.
			NativeSendRequest req = new NativeSendRequest();
			req.handle = nativeRget(wBuffer, numBytes, target_rank, target_disp, numBytes, this.getHandle());

			// Sync wbuffer back to origin_buffer when windows is syncrhonized
			syncManager.AddSync(wBuffer, origin_buffer, 0, 0, origin_count, origin_datatype.getType(), target_rank);

			return req;

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private native long nativeRget(java.nio.ByteBuffer origin_buffer, int origin_count, int target_rank, int target_disp, int target_count, long win);

	public mpjdev.Request rAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		//   return super.Raccumulate(origin_buffer, origin_count, origin_datatype, target_rank, target_disp, target_count, target_datatype, op, win);
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			// I am not sure how the target displacement shud be calculated here.
			// We're providing MPI_BYTE as the data type for the target but the
			// actualy displacement in bytes is calculated based on what the target
			// process provided as disp_unit during window creation. Since MPI is doing
			// this, I have commented out my line.
			// nativeIntracomm.Accumulate(wBuffer, numBytes, target_rank, target_disp, numBytes, op, win);
			NativeSendRequest req = new NativeSendRequest();
			req.handle = nativeRaccumulate(wBuffer, origin_count, origin_datatype.baseType, target_rank, target_disp, target_count, target_datatype.baseType, op.opCode, this.getHandle());
			return req;
			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private native long nativeRaccumulate(java.nio.ByteBuffer origin_buffer, int origin_count, int origin_datatype, int target_rank, int target_disp, int target_count, int target_datatype, int op, long win);

	public mpjdev.Request rGetAccumulate(Object origin_buffer, int origin_count, mpi.Datatype origin_datatype, Object result_buffer, int result_count, mpi.Datatype result_datatype, int target_rank, int target_disp, int target_count, mpi.Datatype target_datatype, mpi.Op op) {
		// for UB, LB, Object and Derived Datatypes (Vector, Struct etc when
		// size>1)
		// Im not sure why we are doing this? A remnant of the past?
		// if (origin_datatype.baseType > PRIMITIVE_TYPE_RANGE_UB || origin_datatype.Size() > 1) {
		//   return super.Rget_accumulate(origin_buffer, origin_count, origin_datatype, result_buffer, result_count, result_datatype, target_rank, target_disp, target_count, target_datatype, op, win);
		// }

		int numBytes = origin_count * origin_datatype.getByteSize();
		ByteBuffer wBuffer = bufferAllocator.allocate(numBytes);
		ByteBuffer wResult = bufferAllocator.allocate(target_count * target_datatype.getByteSize());

		try {
			byteBufferSetData(origin_buffer, wBuffer, 0, 0, origin_count, origin_datatype.getType());
			wBuffer.flip();
			wBuffer.limit(numBytes);

			NativeSendRequest req = new NativeSendRequest();
			req.handle = nativeRgetAccumulate(wBuffer, origin_count, origin_datatype.baseType, wResult, result_count, result_datatype.baseType, target_rank, target_disp, target_count, target_datatype.baseType, op.opCode, this.getHandle());

			// Sync wbuffer back to origin_buffer when windows is syncrhonized
			syncManager.AddSync(wResult, result_buffer, 0, 0, result_count, result_datatype.getType(), target_rank);

			return req;

			// Why you do this? :3
			// wBuffer.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private native long nativeRgetAccumulate(java.nio.ByteBuffer origin_buffer, int origin_count, int origin_datatype, java.nio.ByteBuffer result_buffer, int result_count, int result_datatype, int target_rank, int target_disp, int target_count, int target_datatype, int op, long win);

	public void fence(int assertions) {
		nativeFenceWin(assertions, this.getHandle());
		syncManager.DoSync();
	}

	private native void nativeFenceWin(int assertions, long win);

	public void start(mpi.Group group, int assertions) {
		nativeStartWin(((mpjdev.natmpjdev.Group)group.mpjdevGroup).getHandle(), assertions, this.getHandle());
	}

	private native void nativeStartWin(long group, int assertions, long win);

	public void complete() {
		nativeCompleteWin(this.getHandle());
		syncManager.DoSync();
	}

	private native void nativeCompleteWin(long win);

	public void post(mpi.Group group, int assertions) {
		nativePostWin(((mpjdev.natmpjdev.Group)group.mpjdevGroup).getHandle(), assertions, this.getHandle());
	}

	private native void nativePostWin(long group, int assertions, long win);

	public void waitFor() {
		nativeWaitWin(this.getHandle());
	}

	private native void nativeWaitWin(long win);

	public boolean test() {
		return nativeTestWin(this.getHandle());
	}

	private native boolean nativeTestWin(long win);

	public void lock(int lock_type, int rank, int assertions) {
		nativeLockWin(lock_type, rank, assertions, this.getHandle());
	}

	private native void nativeLockWin(int lock_type, int rank, int assertions, long win);

	public void lockAll(int assertions) {
		nativeLockAllWin(assertions, this.getHandle());
	}

	private native void nativeLockAllWin(int assertions, long win);

	public void unlock(int rank) {
		nativeUnlockWin(rank, this.getHandle());
		syncManager.DoSync(rank);
	}

	private native void nativeUnlockWin(int rank, long win);

	public void unlockAll() {
		nativeUnlockAllWin(this.getHandle());
		syncManager.DoSync();
	}

	private native void nativeUnlockAllWin(long win);

	public void flush(int rank) {
		nativeFlushWin(rank, this.getHandle());
		syncManager.DoSync(rank);
	}

	private native void nativeFlushWin(int rank, long win);

	public void flushAll() {
		nativeFlushAllWin(this.getHandle());
		syncManager.DoSync();
	}

	private native void nativeFlushAllWin(long win);

	public void flushLocal(int rank) {
		nativeFlushLocalWin(rank, this.getHandle());
	}

	private native void nativeFlushLocalWin(int rank, long win);

	public void flushLocalAll() {
		nativeFlushLocalAllWin(this.getHandle());
	}

	private native void nativeFlushLocalAllWin(long win);

	public void sync() {
		nativeSyncWin(this.getHandle());
		syncManager.DoSync();
	}

	private native void nativeSyncWin(long win);

	/**
	 * Helps synchronize temporary buffers back with user buffers at the end
	 * of an RMA epoch.
	 *
	 * RMA operations often require to return a result. Since native MPI calls
	 * can only take a raw memory, we use ByteBuffer as temporary buffers to
	 * receive the results. These results are then lazily copied back to the
	 * user buffer on completion of the RMA epoch (fence, PSCW, lock or flush).
	 *
	 * A SyncItem represents one buffer that needs to be synced back. These
	 * SyncItems are kept in a queue for their respective window.
	 *
	 * NOTE: I haven't yet checked for race conditions but I think AddSync and
	 * DoSync should synchronize on the window's queue when working.
	 */
	private class LocalSyncManager {
		private class SyncItem {
			ByteBuffer src;
			Object dest;
			int srcOffset;
			int offset;
			int count;
			int typeCode;
			int rank;

			SyncItem(ByteBuffer src, Object dest, int srcOffset, int offset, int count, int typeCode, int rank) {
				this.src = src;
				this.dest = dest;
				this.srcOffset = srcOffset;
				this.offset = offset;
				this.count = count;
				this.typeCode = typeCode;
				this.rank = rank;
			}

			void Sync(boolean freeBuffer) {
				byteBufferGetData(src, dest, srcOffset, offset, count, typeCode);
				// src.clear();
				if(freeBuffer)
					bufferAllocator.free(src);
				src = null;
				dest = null;
			}
		}

		private ConcurrentLinkedQueue<SyncItem> syncQueue;

		LocalSyncManager() {
			syncQueue = new ConcurrentLinkedQueue<>();
		}

		void AddSync(ByteBuffer src, Object dest, int srcOffset, int offset, int count, int typeCode, int rank) {
			syncQueue.add(new SyncItem(src, dest, srcOffset, offset, count, typeCode, rank));
		}

		void DoSync() {
			for (SyncItem item : syncQueue) {
				item.Sync(false);
			}
			syncQueue.clear();
			bufferAllocator.freeAll();
		}

		void DoSync(int rank) {
			for (java.util.Iterator<SyncItem> it = syncQueue.iterator(); it.hasNext();) {
				SyncItem item = it.next();
				if (item.rank == rank) {
					item.Sync(true);
					// System.out.println("Item synced");
					it.remove();
				}
			}
		}

	}

	/**
	 * Utility functions
	 */

	private void byteBufferSetData(Object src, ByteBuffer dest, int destOffset, int offset, int count, int typeCode) {

		if (typeCode == this.BYTE) {
			dest.position(destOffset);
			dest.put((byte[]) src, offset, count);
		} else if (typeCode == this.CHAR) {
			dest.position(destOffset);
			CharBuffer CharBuffer = dest.asCharBuffer();
			CharBuffer.put((char[]) src, offset, count);
		} else if (typeCode == this.SHORT) {
			dest.position(destOffset);
			ShortBuffer ShortBuffer = dest.asShortBuffer();
			ShortBuffer.put((short[]) src, offset, count);
		} else if (typeCode == this.BOOLEAN) {
			dest.position(destOffset);
			boolean srcB[] = (boolean[]) src;
			for (int i = 0; i < count; i++) {
				dest.put((byte) (srcB[i + offset] ? 1 : 0)); // boolean
			}

		} else if (typeCode == this.INT) {
			dest.position(destOffset);
			IntBuffer IntBuffer = dest.asIntBuffer();
			IntBuffer.put((int[]) src, offset, count);
		} else if (typeCode == this.LONG) {
			dest.position(destOffset);
			LongBuffer LongBuffer = dest.asLongBuffer();
			LongBuffer.put((long[]) src, offset, count);
		} else if (typeCode == this.FLOAT) {
			dest.position(destOffset);
			FloatBuffer FloatBuffer = dest.asFloatBuffer();
			FloatBuffer.put((float[]) src, offset, count);
		} else if (typeCode == this.DOUBLE) {
			dest.position(destOffset);
			DoubleBuffer DoubleBuffer = dest.asDoubleBuffer();
			DoubleBuffer.put((double[]) src, offset, count);
		}

	}

	private void byteBufferGetData(ByteBuffer src, Object dest, int srcOffset, int offset, int count, int typeCode) {

		if (typeCode == this.BYTE) {
			src.position(srcOffset);
			src.get((byte[]) dest, offset, count);
		} else if (typeCode == this.CHAR) {
			src.position(srcOffset);
			CharBuffer CharBuffer = src.asCharBuffer();
			CharBuffer.get((char[]) dest, offset, count);
		} else if (typeCode == this.SHORT) {
			src.position(srcOffset);
			ShortBuffer ShortBuffer = src.asShortBuffer();
			ShortBuffer.get((short[]) dest, offset, count);
		} else if (typeCode == this.BOOLEAN) {
			src.position(srcOffset);
			boolean destB[] = (boolean[]) dest;

			for (int i = 0; i < count; i++) {
				destB[i + offset] = (src.get() == 1); // boolean
			}

		} else if (typeCode == this.INT) {
			src.position(srcOffset);
			IntBuffer IntBuffer = src.asIntBuffer();
			IntBuffer.get((int[]) dest, offset, count);
		} else if (typeCode == this.LONG) {
			src.position(srcOffset);
			LongBuffer LongBuffer = src.asLongBuffer();
			LongBuffer.get((long[]) dest, offset, count);
		} else if (typeCode == this.FLOAT) {
			src.position(srcOffset);
			FloatBuffer FloatBuffer = src.asFloatBuffer();
			FloatBuffer.get((float[]) dest, offset, count);
		} else if (typeCode == this.DOUBLE) {
			src.position(srcOffset);
			DoubleBuffer DoubleBuffer = src.asDoubleBuffer();
			DoubleBuffer.get((double[]) dest, offset, count);
		}

	}

}