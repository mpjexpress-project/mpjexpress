#include <stdlib.h>
#include <string.h>
#include "mpi.h"
#include "mpjdev_natmpjdev_Win.h"
#include "mpjdev_natmpjdev_shared.h"


JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeCreateWin
(JNIEnv *env, jobject thisClass, jobject base, jint size, jint disp_unit, jlong comm) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, base);
	MPI_Comm mpi_comm = (MPI_Comm) comm;
	MPI_Win *win = malloc(sizeof(MPI_Win));
	MPI_Win_create(baseptr, size, disp_unit, MPI_INFO_NULL, mpi_comm, win);
	return (jlong)win;
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeAllocWin
(JNIEnv *env, jobject thisClass, jint size, jint disp_unit, jlong comm) {

	void *base;
	MPI_Comm mpi_comm = (MPI_Comm) comm;
	MPI_Win *win = malloc(sizeof(MPI_Win));
	MPI_Win_allocate(size, disp_unit, MPI_INFO_NULL, mpi_comm, &base, win);
	return (jlong)win;
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeCreateDynamicWin
(JNIEnv *env, jobject thisClass, jlong comm) {

	MPI_Comm mpi_comm = (MPI_Comm) comm;
	MPI_Win *win = malloc(sizeof(MPI_Win));
	MPI_Win_create_dynamic(MPI_INFO_NULL, mpi_comm, win);
	return (jlong)win;
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeAttachWin
(JNIEnv *env, jobject thisClass, jobject base, jint size, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, base);
	MPI_Win_attach(*((MPI_Win *)win), baseptr, size);
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeDetachWin
(JNIEnv *env, jobject thisClass, jobject base, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, base);
	MPI_Win_detach(*((MPI_Win *)win), baseptr);
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFreeWin
(JNIEnv *env, jobject thisClass, jlong win) {

	MPI_Win_free((MPI_Win *)win);
	free((MPI_Win *)win);
}

JNIEXPORT jobject JNICALL Java_mpjdev_natmpjdev_Win_nativeGetBase
(JNIEnv *env, jobject thisClass, jlong win) {

	void *base;
	int flag = 0;
	int *size = 0;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_BASE, &base, &flag);
	flag = 0;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_SIZE, &size, &flag);
	return (*env)->NewDirectByteBuffer(env, base, *size);
}

JNIEXPORT jint JNICALL Java_mpjdev_natmpjdev_Win_nativeGetSize
(JNIEnv *env, jobject thisClass, jlong win) {

	int flag = 0;
	int *size;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_SIZE, &size, &flag);
	return *size;
}

JNIEXPORT jint JNICALL Java_mpjdev_natmpjdev_Win_nativeGetDispUnit
(JNIEnv *env, jobject thisClass, jlong win) {

	int flag = 0, *disp_unit;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_DISP_UNIT, &disp_unit, &flag);
	return *disp_unit;
}

JNIEXPORT jint JNICALL Java_mpjdev_natmpjdev_Win_nativeGetCreateFlavor
(JNIEnv *env, jobject thisClass, jlong win) {

	int flag = 0, *create_kind;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_CREATE_FLAVOR, &create_kind, &flag);
	return convertToMPJWinFlavor(*create_kind);
}

JNIEXPORT jint JNICALL Java_mpjdev_natmpjdev_Win_nativeGetModel
(JNIEnv *env, jobject thisClass, jlong win) {

	int flag = 0, *memory_model;
	MPI_Win_get_attr(*((MPI_Win *)win), MPI_WIN_MODEL, &memory_model, &flag);
	return convertToMPJWinModel(*memory_model);
}


JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativePut
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint target_rank, jint target_disp, jint target_count, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	MPI_Put(baseptr, origin_count, MPI_BYTE, target_rank, target_disp, target_count, MPI_BYTE, *((MPI_Win *)win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeGet
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint target_rank, jint target_disp, jint target_count, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	MPI_Get(baseptr, origin_count, MPI_BYTE, target_rank, target_disp, target_count, MPI_BYTE, *((MPI_Win *)win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeAccumulate
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint origin_datatype, jint target_rank, jint target_disp, jint target_count, jint target_datatype, jint op, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	int err = MPI_Accumulate(baseptr, origin_count, convertToMPIDataType(origin_datatype), target_rank, target_disp, target_count, convertToMPIDataType(target_datatype), convertToMPIOp(op), *((MPI_Win *)win));
	if (err)
		printf("Error in accumulate\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeGetAccumulate
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint origin_datatype, jobject result_buffer, jint result_count, jint result_datatype, jint target_rank, jint target_disp, jint target_count, jint target_datatype, jint op, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	void *resultptr = (*env)->GetDirectBufferAddress(env, result_buffer);
	int err = MPI_Get_accumulate(baseptr, origin_count, convertToMPIDataType(origin_datatype), resultptr, result_count, convertToMPIDataType(result_datatype), target_rank, target_disp, target_count, convertToMPIDataType(target_datatype), convertToMPIOp(op), *((MPI_Win *)win));
	if (err)
		printf("Error in get accumulate\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFetchAndOp
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jobject result_buffer, jint datatype, jint target_rank, jint target_disp, jint op, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	void *resultptr = (*env)->GetDirectBufferAddress(env, result_buffer);
	int err = MPI_Fetch_and_op(baseptr, resultptr, convertToMPIDataType(datatype), target_rank, target_disp, convertToMPIOp(op), *((MPI_Win *)win));
	if (err)
		printf("Error in fetch and op\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeCompareAndSwap
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jobject compare_buffer, jobject result_buffer, jint datatype, jint target_rank, jint target_disp, jlong win) {

	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	void *compareptr = (*env)->GetDirectBufferAddress(env, compare_buffer);
	void *resultptr = (*env)->GetDirectBufferAddress(env, result_buffer);
	int err = MPI_Compare_and_swap(baseptr, compareptr, resultptr, convertToMPIDataType(datatype), target_rank, target_disp, *((MPI_Win *)win));
	if (err)
		printf("Error in compare and swap\n");
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeRput
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint target_rank, jint target_disp, jint target_count, jlong win) {

	MPI_Request req;
	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	MPI_Rput(baseptr, origin_count, MPI_BYTE, target_rank, target_disp, target_count, MPI_BYTE, *((MPI_Win *)win), &req);
	return (jlong)req;
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeRget
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint target_rank, jint target_disp, jint target_count, jlong win) {

	MPI_Request req;
	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	MPI_Rget(baseptr, origin_count, MPI_BYTE, target_rank, target_disp, target_count, MPI_BYTE, *((MPI_Win *)win), &req);
	return (jlong)req;
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeRaccumulate
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint origin_datatype, jint target_rank, jint target_disp, jint target_count, jint target_datatype, jint op, jlong win) {

	MPI_Request req;
	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	int err = MPI_Raccumulate(baseptr, origin_count, convertToMPIDataType(origin_datatype), target_rank, target_disp, target_count, convertToMPIDataType(target_datatype), convertToMPIOp(op), *((MPI_Win *)win), &req);
	if (err)
		printf("Error in accumulate\n");
	return (jlong)req;
}

JNIEXPORT jlong JNICALL Java_mpjdev_natmpjdev_Win_nativeRgetAccumulate
(JNIEnv *env, jobject thisClass, jobject origin_buffer, jint origin_count, jint origin_datatype, jobject result_buffer, jint result_count, jint result_datatype, jint target_rank, jint target_disp, jint target_count, jint target_datatype, jint op, jlong win) {

	MPI_Request req;
	void *baseptr = (*env)->GetDirectBufferAddress(env, origin_buffer);
	void *resultptr = (*env)->GetDirectBufferAddress(env, result_buffer);
	int err = MPI_Rget_accumulate(baseptr, origin_count, convertToMPIDataType(origin_datatype), resultptr, result_count, convertToMPIDataType(result_datatype), target_rank, target_disp, target_count, convertToMPIDataType(target_datatype), convertToMPIOp(op), *((MPI_Win *)win), &req);
	if (err)
		printf("Error in get accumulate\n");
	return (jlong)req;
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFenceWin
(JNIEnv *env, jobject thisClass, jint assertions, jlong win) {

	MPI_Win_fence(convertToMPIAssert(assertions), *((MPI_Win *)win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeStartWin
(JNIEnv *env, jobject thisClass, jlong group, jint assertions, jlong win) {

	int err = MPI_Win_start((MPI_Group) group, convertToMPIAssert(assertions), *((MPI_Win *) win));
	if (err)
		printf("error in MPI_Win_start\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeCompleteWin
(JNIEnv *env, jobject thisClass, jlong win) {
	int err = MPI_Win_complete(*((MPI_Win *) win));
	if (err)
		printf("error in MPI_Win_complete\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativePostWin
(JNIEnv *env, jobject thisClass, jlong group, jint assertions, jlong win) {

	int err = MPI_Win_post((MPI_Group) group, convertToMPIAssert(assertions), *((MPI_Win *) win));
	if (err)
		printf("error in MPI_Win_post\n");
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeWaitWin
(JNIEnv *env, jobject thisClass, jlong win) {

	int err = MPI_Win_wait(*((MPI_Win *) win));
	if (err)
		printf("error in MPI_Win_wait\n");
}

JNIEXPORT jboolean JNICALL Java_mpjdev_natmpjdev_Win_nativeTestWin
(JNIEnv *env, jobject thisClass, jlong win) {

	int flag = 0;
	MPI_Win_test(*((MPI_Win *) win), &flag);
	if (flag)
		return JNI_TRUE;
	else
		return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeLockWin
(JNIEnv *env, jobject thisClass, jint locktype, jint rank, jint assertions, jlong win) {

	if (locktype == MPJ_LOCK_SHARED)
		MPI_Win_lock(MPI_LOCK_SHARED, rank, convertToMPIAssert(assertions), *((MPI_Win *) win));
	else
		MPI_Win_lock(MPI_LOCK_EXCLUSIVE, rank, convertToMPIAssert(assertions), *((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeLockAllWin
(JNIEnv *env, jobject thisClass, jint assertions, jlong win) {

	MPI_Win_lock_all(convertToMPIAssert(assertions), *((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeUnlockWin
(JNIEnv *env, jobject thisClass, jint rank, jlong win) {

	MPI_Win_unlock(rank, *((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeUnlockAllWin
(JNIEnv *env, jobject thisClass, jlong win) {

	MPI_Win_unlock_all(*((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFlushWin
(JNIEnv *env, jobject thisClass, jint rank, jlong win) {

	MPI_Win_flush(rank, *((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFlushAllWin
(JNIEnv *env, jobject thisClass, jlong win) {

	MPI_Win_flush_all(*((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFlushLocalWin
(JNIEnv *env, jobject thisClass, jint rank, jlong win) {

	MPI_Win_flush_local(rank, *((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeFlushLocalAllWin
(JNIEnv *env, jobject thisClass, jlong win) {

	MPI_Win_flush_local_all(*((MPI_Win *) win));
}

JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_Win_nativeSyncWin
(JNIEnv *env, jobject thisClass, jlong win) {

	MPI_Win_sync(*((MPI_Win *) win));
}



