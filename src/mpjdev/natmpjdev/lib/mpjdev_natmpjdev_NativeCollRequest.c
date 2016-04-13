
#include "mpi.h"
#include "mpjdev_natmpjdev_NativeCollRequest.h"
#include "mpjdev_natmpjdev_shared.h"


jfieldID reqhandleID;


/*
 * Class:     mpjdev_natmpjdev_NativeCollRequest
 * Method:    initNativeCollRequest
 * Signature: ()V
 */


JNIEXPORT void JNICALL Java_mpjdev_natmpjdev_NativeCollRequest_initNativeCollRequest
  (JNIEnv *env, jclass thisClass)
  {

  //this can go in JNI_onLOAD
  jclass CL_mpjdev_natmpjdev_NativeRequest = (*env)->FindClass(env,
      "mpjdev/natmpjdev/NativeRequest");

  reqhandleID = (*env)->GetFieldID(env, CL_mpjdev_natmpjdev_NativeRequest, "handle",
      "J");


  }

/*
 * Class:     mpjdev_natmpjdev_NativeCollRequest
 * Method:    Wait
 * Signature: (Lmpjdev/Status;)Lmpjdev/Status;
 */
JNIEXPORT jobject JNICALL Java_mpjdev_natmpjdev_NativeCollRequest_Wait
  (JNIEnv *env, jobject thisObject, jobject stat)
  {

    int elements;

  MPI_Request request = (MPI_Request)(
      (*env)->GetLongField(env, thisObject, reqhandleID));

  MPI_Status mpi_status;
   // printf("BWRequest: %p\n", (void *) request );


  int err = MPI_Wait(&request, &mpi_status);
  if(err)
    printf("Error in wait\n");

  MPI_Get_count(&mpi_status, MPI_BYTE, &elements);

  (*env)->SetIntField(env, stat, mpjdev_Status_sourceID, mpi_status.MPI_SOURCE);
  (*env)->SetIntField(env, stat, mpjdev_Status_tagID, mpi_status.MPI_TAG);
  (*env)->SetIntField(env, stat, mpjdev_Status_numEls, elements);

  // I am also setting this because we need it in mpi/Status Get_count
  // method : TODO remove magic numbers
  (*env)->SetIntField(env, stat, mpjdev_Status_countInBytes,
      (jint)(elements - 8 - 8));

   //printf("WRequest: %p\n", (void *) request );


  return stat;


  }

/*
 * Class:     mpjdev_natmpjdev_NativeCollRequest
 * Method:    Test
 * Signature: (Lmpjdev/Status;)Lmpjdev/Status;
 */
JNIEXPORT jobject JNICALL Java_mpjdev_natmpjdev_NativeCollRequest_Test
  (JNIEnv *env, jobject thisObject, jobject stat)


  {

    int flag;

  MPI_Request request = (MPI_Request)(
      (*env)->GetLongField(env, thisObject, reqhandleID));

  MPI_Status mpi_status;

  MPI_Test(&request, &flag, &mpi_status);

  if (flag) {
    int elements;
    //TODO get count or what?
    (*env)->SetIntField(env, stat, mpjdev_Status_sourceID,
        mpi_status.MPI_SOURCE);
    (*env)->SetIntField(env, stat, mpjdev_Status_tagID, mpi_status.MPI_TAG);

    //? MPI_Get_count(&mpi_status, MPI_BYTE, &elements);
    // (*env)->SetIntField(env, stat, mpjdev_Status_numEls, elements);

    
    return stat;

  } else
    return NULL;


  }