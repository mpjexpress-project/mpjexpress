/* ********************************************************************
 * KaRMI
 *
 * Copyright (C) 1998-2002 The JavaParty Team, University of Karlsruhe
 *
 * Permission is hereby granted to use and modify this software.
 * The software, or modifications thereof, may be redistributed only
 * if the source code is also provided and this copyright notice stays 
 * attached.
 **********************************************************************/
/*
 * $Revision: 1.2 $
 * $Date: 2005/03/24 10:49:51 $
 */

#include <xdev_gmdev_GM.h>
#include <gm.h>

#include <stdlib.h>
#include <string.h>

#define debugprint(...) 
// #define debugprint printf

/*
 * if something goes wrong, we abort
 */
void err_exit(char * s, gm_status_t status)
{
  fprintf(stderr,"%s", s);
  if (status != GM_SUCCESS)
    fprintf(stderr,": %s.\n", gm_strerror(status));
  else
    fprintf(stderr,".\n");
  exit(EXIT_FAILURE);
}

jclass IOException = 0;
jclass InternalError = 0;

#define sizesCnt 13
#define supportedSizesCnt 4

//                                     0,   1,  2 , 3 , 4,  5,  6,  7,  8,  9, 10, 11, 12
const unsigned int supportedSizes[] = {12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12};

struct hportInfo {
  /** array of booleans, true if a receive request for vport could not
      be fulfilled and the corresponding thread starts waiting */
  unsigned char waiting[xdev_gmdev_GM_VPORTS];

  /** list of buffers already filled in receive events per port */
  struct receiveBuffer * receivedBuffers[xdev_gmdev_GM_VPORTS];

  /** pointer to the last buffer in the receivedBuffers list per port */
  struct receiveBuffer * lastReceivedBuffer[xdev_gmdev_GM_VPORTS];

  struct receiveBuffer * firstReceivedBufferHead;
  struct receiveBuffer * lastReceivedBufferHead;

  /** amount of buffers passed to the GM library for receive per port */
  unsigned int providedBuffersCnt[sizesCnt];

  /** list of free send buffers per port */
  struct sendBuffer * sendBuffer[sizesCnt];

  /** per port */
  unsigned int receiveToken[sizesCnt];

  /** per port */
  unsigned int sendToken[sizesCnt];

  struct gm_port * gm_port;

  /** portnr */
  unsigned int hportNr;

};

jint findSignal(struct hportInfo * port);

#define hportsCnt 8
struct hportInfo hports[hportsCnt];

/** guaranteed amount of threads supported per port */
unsigned int threads; 

long long rticks = 0, sticks = 0, gticks = 0;
long rcnt = 0, scnt = 0;
int maxQueueLen = 0;
int unknownCnt = 0;

struct transmit {
  jint vportFrom;
  jint vportTo;
  char data[0];
};

struct sendBuffer {
  // memory management
  struct sendBuffer * next;
  unsigned int size;
  jint hport;

  struct transmit tx;
};

const unsigned int MAGIC = 0x474D5445;

struct receiveBuffer {
  unsigned int magic;
  // memory management
  /** next message for the same vport */
  struct receiveBuffer * next;

  struct receiveBuffer * nextHead;
  struct receiveBuffer * prevHead;

  unsigned int size;

  // calculated from event
  /** length of user message */
  jint length;
  jint nodeFrom;

  struct transmit tx;
};

// There is a list of reveived buffers for each
// vport. receivedBuffers[vport] points to the head of this list and
// lastReceivedBuffer[vport] points to the last buffer in this list.

// The heads of these lists are linked in another list used to keep
// track of all vports where buffers are waiting. This list is a
// double linked list to be able to remove an element anywhere in the
// list. firstReceivedBufferHead points to the first element and
// lastReceivedBuffer points to the last element in this list.

// In contrast to the following picture the list of the heads is not
// required to be sorted for vports. This list keeps the buffers in
// the order they are received.

//                             firstReceivedBufferHead
//                               |
// receivedBuffers[vport=0] -|   |                        |- lastReceivedBuffer[vport=0]
//                               |
//                               |
// receivedBuffers[vport=1] -|   |                        |- lastReceivedBuffer[vport=1]
//                               |
//                               v
// receivedBuffers[vport=2] -> buffer -> buffer -> buffer <- lastReceivedBuffer[vport=2]
//                               |
//                               |
// receivedBuffers[vport=3] -|   |                        |- lastReceivedBuffer[vport=3]
//                               |
//                               v
// receivedBuffers[vport=4] -> buffer <--------------------- lastReceivedBuffer[vport=4]
//                               |
//                               v
// receivedBuffers[vport=5] -> buffer -> buffer <----------- lastReceivedBuffer[vport=5]
//                               ^
//                               |
//                             lastReceivedBufferHead

// prevHead |
//          v    next
//     -> buffer -> 
//          |
//          v nextHead

inline void queueBuffer(struct receiveBuffer * buffer, struct hportInfo * port, jint vport) {
  if (port->lastReceivedBuffer[vport]) {
    // append to non-empty list for vport
    port->lastReceivedBuffer[vport]->next = buffer;
    port->lastReceivedBuffer[vport] = buffer;
    buffer->prevHead = 0;
    buffer->nextHead = 0;    
  } else {
    // empty list for vport
    port->lastReceivedBuffer[vport] = port->receivedBuffers[vport] = buffer;

    // append to heads
    if (port->lastReceivedBufferHead) {
      // append to non-empty list
      buffer->prevHead = port->lastReceivedBufferHead;
      buffer->nextHead = 0;
      port->lastReceivedBufferHead->nextHead = buffer;
      port->lastReceivedBufferHead = buffer;
    } else {
      // empty list
      port->lastReceivedBufferHead = port->firstReceivedBufferHead = buffer;
      buffer->prevHead = 0;
      buffer->nextHead = 0;
    }
  }
  buffer->next = 0;
}

inline struct receiveBuffer * unqueueBuffer(struct hportInfo * port, jint vport) {
  // buffer to be removed from the structure
  struct receiveBuffer * buffer = port->receivedBuffers[vport];

  // remove buffer from the list of waiting buffers for vport
  port->receivedBuffers[vport] = buffer->next;
  buffer->next = 0;

  if (! port->receivedBuffers[vport]) {
    // last buffer removed for vport
    port->lastReceivedBuffer[vport] = 0;

    if (buffer->prevHead) {
      buffer->prevHead->nextHead = buffer->nextHead;
    } else {
      port->firstReceivedBufferHead = buffer->nextHead;
    }

    if (buffer->nextHead) {
      buffer->nextHead->prevHead = buffer->prevHead;
    } else {
      port->lastReceivedBufferHead = buffer->prevHead;
    }
  } else {
    // there are more buffers for vport waiting

    if (buffer->prevHead) {
      buffer->prevHead->nextHead = port->receivedBuffers[vport];
    } else {
      port->firstReceivedBufferHead = port->receivedBuffers[vport];
    }

    if (buffer->nextHead) {
      buffer->nextHead->prevHead = port->receivedBuffers[vport];
    } else {
      port->lastReceivedBufferHead = port->receivedBuffers[vport];
    }
    //Initialize the prevHead and nextHead pointers of the next
    //waiting buffer.
    port->receivedBuffers[vport]->prevHead = buffer->prevHead;
    port->receivedBuffers[vport]->nextHead = buffer->nextHead;
    
  }
  buffer->nextHead = 0;
  buffer->prevHead = 0;

  return buffer;
}

inline unsigned int getSupportedSize(gm_size_t length) {
  unsigned int size = gm_min_size_for_length(length);
  return supportedSizes[size];
}

inline jlong createSignalAndLength(jint signal, jint length) {
  return ((((jlong) signal) & 0xFFFFFFFF) << 32) | (((jlong) length) & 0xFFFFFFFF);
}

static void debugPrintMsgQueue(struct hportInfo * port, jint vport) {
  struct receiveBuffer  *buffer;

  buffer = port->receivedBuffers[vport];
  while (buffer) {
    jint vportTo = buffer->tx.vportTo;
    debugprint("findSignal() waiting message for vport=%d, size=%d, length=%d\n", vportTo, buffer->size, buffer->length);

    buffer = buffer->next;
  }
}

/** character buffer for assembling strings using sprintf */
char cbuf[1024];

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_GM_gmInit
  (JNIEnv * env, jclass clazz)
{
  gm_status_t status;

  debugprint("gmInit()\n");

  // load exception classes
  IOException = (*env)->FindClass(env, "java/io/IOException");
  InternalError = (*env)->FindClass(env, "java/lang/InternalError");

  IOException = (*env)->NewGlobalRef(env, IOException);
  InternalError = (*env)->NewGlobalRef(env, InternalError);

  if ((status = gm_init()) != GM_SUCCESS) {
    sprintf(cbuf, "gm_init() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
    return;
  }

  debugprint("finished gmInit()\n");
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmFinalize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_GM_gmFinalize
  (JNIEnv * env, jclass clazz)
{
  gm_finalize();
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmOpen
 * Signature: (IILjava/lang/String
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_GM_gmOpen
  (JNIEnv * env, jclass clazz, jint device, jint hport, jstring name)
{
  const char * _name = (*env)->GetStringUTFChars(env, name, 0);
  gm_status_t status;
  int n;
  struct hportInfo * port = &(hports[hport]);

  debugprint("gmOpen()\n");

  if ((status = gm_open(&(port->gm_port), device, hport, _name, GM_API_VERSION)) != GM_SUCCESS) {
    sprintf(cbuf, "gm_open() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
    return;
  }

  (*env)->ReleaseStringUTFChars(env, name, _name);

  port->hportNr = hport;

  for (n = 0; n < sizesCnt; n++) {
    port->sendBuffer[n] = 0;

    port->receiveToken[n] = 0;
    port->sendToken[n] = 0;

    port->providedBuffersCnt[n] = 0;
  }

  // allocate receive buffers
  {
    int rxtokens = gm_num_receive_tokens(port->gm_port);
    debugprint("gmOpen() rxtokens=%d\n", rxtokens);
    rxtokens /= sizesCnt;

    // spread tokens for supported sizes
    for (n = 0; n < sizesCnt; n++) {
      port->receiveToken[supportedSizes[n]] += rxtokens;
    }

    {
      unsigned int rxthreads = rxtokens * sizesCnt;

      for (n = 0; n < sizesCnt; n++) {
	if (port->receiveToken[supportedSizes[n]] < rxthreads) {
	  rxthreads = port->receiveToken[supportedSizes[n]];
	}
      }

      threads = rxthreads;
    }
  }

  port->firstReceivedBufferHead = 0;
  port->lastReceivedBufferHead = 0;

  for (n = 0; n < xdev_gmdev_GM_VPORTS; n++) {
    port->waiting[n] = 0;
 
    port->receivedBuffers[n] = 0;
    port->lastReceivedBuffer[n] = 0;
  }

  for (n = 0; n < sizesCnt; n++) {
    while (port->receiveToken[n] > 0) {
	struct receiveBuffer * newbuffer = gm_dma_malloc(port->gm_port, gm_max_length_for_size(n) + sizeof(struct receiveBuffer));

	if (! newbuffer) {
	  debugprint("gmOpen() panic: can not allocate receive buffer for size=%d\n", n);
	}
	debugprint("gmOpen() creating new buffer size=%d\n", n);

	newbuffer->magic = MAGIC;
	newbuffer->next = 0;
	newbuffer->nextHead = 0;
	newbuffer->prevHead = 0;
	newbuffer->size = n;
  
	gm_provide_receive_buffer(port->gm_port, &(newbuffer->tx), n, GM_LOW_PRIORITY);
	port->providedBuffersCnt[n]++;
	port->receiveToken[n]--;
    }
  }

  {
    int txtokens = gm_num_send_tokens(port->gm_port);
    debugprint("gmOpen() txtokens=%d\n", txtokens);
    txtokens /= sizesCnt;
    for (n = 0; n < sizesCnt; n++) {
      port->sendToken[supportedSizes[n]] += txtokens;

      while (port->sendToken[n] > 0) {
	struct sendBuffer * buffer;

	debugprint("gmOpen() before gm_dma_malloc()\n");
	buffer = gm_dma_malloc(port->gm_port, gm_max_length_for_size(n) + sizeof(struct sendBuffer));
	debugprint("gmOpen() after gm_dma_malloc()\n");
	if (! buffer) {
	  (*env)->ThrowNew(env, IOException, "no more DMA memory");
	  return;
	}
	port->sendToken[n]--;

	buffer->next = 0;
	buffer->size = n;
	buffer->hport = hport;

	// insert buffer into list of free send buffers for size n
	buffer->next = port->sendBuffer[n];
	port->sendBuffer[n] = buffer;
      }
    }
  }

  debugprint("finished gmOpen()\n");
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmClose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_GM_gmClose
  (JNIEnv * env, jclass clazz, jint hport)
{
  struct hportInfo * port = &(hports[hport]);

  gm_close(port->gm_port);
}

jint processOneEvent(JNIEnv * env, struct hportInfo * port) {
  debugprint("processOneEvent()\n");
  while (1) {
    gm_recv_event_t * evt;
    gm_u8_t evtType;

    // debugprint("processOneEvent() before gm_ticks()\n");
    // gticks -= gm_ticks(port->gm_port);
    debugprint("processOneEvent() before gm_receive() for port %d\n",port->hportNr);
    evt = gm_receive(port->gm_port);
    debugprint("processOneEvent() after gm_receive() for port %d\n",port->hportNr);
    // gticks += gm_ticks(port->gm_port);
    // debugprint("processOneEvent() after gm_ticks()\n");

    evtType = GM_RECV_EVENT_TYPE(evt);
    if (evtType == GM_RECV_EVENT) {
      struct receiveBuffer *buffer = (struct receiveBuffer *)
	(gm_ntohp(evt->recv.buffer) - 
	 (sizeof(struct receiveBuffer) - sizeof(struct transmit)));

      // debugprint("processOneEvent() got receive event\n");

      // copy event information
      buffer->length = gm_ntohl(evt->recv.length) - sizeof(struct transmit);
      buffer->nodeFrom = gm_ntohs(evt->recv.sender_node_id);

      if (gm_ntohc(evt->recv.size) != buffer->size) {
	(*env)->ThrowNew(env, InternalError, "panic: size mismatch");
	return 0;
      }

      port->providedBuffersCnt[buffer->size]--;

      // memory management
      queueBuffer(buffer, port, buffer->tx.vportTo);

      // debugprint("finished processOneEvent()\n");
      return 1;
    } else if (evtType == GM_NO_RECV_EVENT) {
      return 0;
    } else {
      debugprint("processOneEvent() before gm_unknown()\n");
      gm_unknown(port->gm_port, evt);
      debugprint("processOneEvent() after gm_unknown()\n");
    }
  }
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmReceive
 * Signature: (II[BII)J
 */
JNIEXPORT jlong JNICALL Java_xdev_gmdev_GM_gmReceive
  (JNIEnv * env, jclass clazz, jint hport, jint vport, jbyteArray msg, jint offset, jint length)
{
  gm_size_t receiveLength = length + sizeof(struct transmit);
  unsigned int size = getSupportedSize(receiveLength);
  int n;
  struct hportInfo * port = &(hports[hport]);

  debugprint("gmReceive() vport=%d length=%d\n", vport, length);
  while (! port->receivedBuffers[vport]) {
    if (! processOneEvent(env, port)) {
      int signal = findSignal(port);
      port->waiting[vport] = 1;

      return createSignalAndLength(signal, -1);
    }
  }

  {
    // buffer for vport waiting
    struct receiveBuffer * buffer = unqueueBuffer(port, vport);
    jbyte * _msg;

    int receivedLength = buffer->length;
    if (receivedLength > length) {      
      (*env)->ThrowNew(env, IOException, "received message to large");
      return 0;
    }

    // copy message
    _msg = (*env)->GetPrimitiveArrayCritical(env, msg, 0);
    memcpy(_msg + offset, buffer->tx.data, receivedLength);
    (*env)->ReleasePrimitiveArrayCritical(env, msg, _msg, 0);

    // pass buffer back to GM
    debugprint("gmReceive() before gm_provide_receive_buffer()\n");
    gm_provide_receive_buffer(port->gm_port, &(buffer->tx), buffer->size, GM_LOW_PRIORITY);
    debugprint("gmReceive() after gm_provide_receive_buffer()\n");
    port->providedBuffersCnt[buffer->size]++;

    port->waiting[vport] = 0;
    return createSignalAndLength(findSignal(port), receivedLength);
  }
}

void sendCB(struct gm_port * p, void * context, gm_status_t status) {
  struct sendBuffer * buffer = (struct sendBuffer *) context;
  struct hportInfo * port = &(hports[buffer->hport]);

  buffer->next = port->sendBuffer[buffer->size];
  port->sendBuffer[buffer->size] = buffer;
}

jint findSignal(struct hportInfo * port) {
  struct receiveBuffer * buffer;

  //debugprint("findSignal() \n");

  buffer = port->firstReceivedBufferHead;
  while (buffer) {
    jint vportTo = buffer->tx.vportTo;

    if (buffer->magic != MAGIC) {
      printf("panic: not a receive buffer <--------------------------- \n");
    }

    if (port->waiting[vportTo]) {
      // reset signal request
      port->waiting[vportTo] = 0;

      // debugprint("finished findSignal() signal found for vport %d\n", vportTo);
      return vportTo;
    }

    buffer = buffer->nextHead;
  }
  // debugprint("finished findSignal() no signal found\n");

  // no signal 
  return 0;
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmSend
 * Signature: (IIII[BII)J
 */
JNIEXPORT jlong JNICALL Java_xdev_gmdev_GM_gmSend
  (JNIEnv * env, jclass clazz, jint hport, jint vport, jint nodeTo, jint hportTo, jint vportTo, jbyteArray msg, jint offset, jint length)
{
  struct hportInfo * port = &(hports[hport]);
  struct sendBuffer * buffer;
  gm_size_t sendLength = length + sizeof(struct transmit);
  unsigned int size = getSupportedSize(sendLength);
  const int maxEvtCnt = 10;
  int evtCnt = maxEvtCnt;

  debugprint("gmSend(length=%d) \n", length);

  scnt++;

  while ((evtCnt--) && processOneEvent(env, port))
    ;

  if (port->sendBuffer[size] == 0) {
    // no more send buffers for size
    return createSignalAndLength(findSignal(port), -1);
  } else {
    // debugprint("gmSend() remove buffer from list of free send buffers\n");
    // remove buffer from list of free send buffers
    buffer = port->sendBuffer[size];
    port->sendBuffer[size] = buffer->next;
  }

  {
    jbyte * _msg;

    // debugprint("gmSend() copy message\n");
    buffer->tx.vportFrom = vport;
    buffer->tx.vportTo = vportTo;
    _msg = (*env)->GetPrimitiveArrayCritical(env, msg, 0);
    memcpy(buffer->tx.data, _msg + offset, length);
    (*env)->ReleasePrimitiveArrayCritical(env, msg, _msg, JNI_ABORT);

    debugprint("gmSend() before gm_send_with_callback()\n");

    gm_send_with_callback(port->gm_port, &(buffer->tx), size, sendLength, GM_LOW_PRIORITY, nodeTo, hportTo, sendCB, buffer);

    debugprint("gmSend() after gm_send_with_callback()\n");
  }

  // debugprint("finished gmSend()\n");
  return createSignalAndLength(findSignal(port), 0);
}


/*
 * Class:     xdev_gmdev_GM
 * Method:    gmNumPorts
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmNumPorts
  (JNIEnv * env, jclass clazz, jint hport)
{
  return 8;
}


/*
 * Class:     xdev_gmdev_GM
 * Method:    gmMaxMessageSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmMaxMessageSize
  (JNIEnv * env, jclass clazz)
{
  debugprint("gmMaxMessageSize() = %ld \n", gm_max_length_for_size(sizesCnt - 1) - sizeof(struct transmit));
  return gm_max_length_for_size(sizesCnt - 1) - sizeof(struct transmit);
}

/*
 * Class:     xdev_gmdev_GM
 * Method:    gmMaxThreads
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmMaxThreads
  (JNIEnv * env, jclass clazz, jint hport)
{
  return threads;
}


/*
 * Class:     xdev_gmdev_GM
 * Method:    gmNodeID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmGetNodeID__I
  (JNIEnv * env, jclass clazz, jint hport)
{
  gm_status_t status;
  unsigned int id;
  struct hportInfo * port = &(hports[hport]);

  status = gm_get_node_id(port->gm_port, &id);
  if (status != GM_SUCCESS) {
    sprintf(cbuf, "gm_get_node_id() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
    return 0;
  }

  return (jint) id;
}


JNIEXPORT jstring JNICALL Java_xdev_gmdev_GM_gmGetHostName
  (JNIEnv * env, jclass clazz, jint hport)
{
  gm_status_t status;
  char name[GM_MAX_HOST_NAME_LEN];
  jstring result;
  struct hportInfo * port = &(hports[hport]);

  name[0] = '\0';
  status = gm_get_host_name(port->gm_port, name);
  if (status != GM_SUCCESS) {
    sprintf(cbuf, "gm_get_host_name() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
    return 0;
  }

  result = (*env)->NewStringUTF(env, name);
  return result;
}

JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmGetNodeID__ILjava_lang_String_2
  (JNIEnv * env, jclass clazz, jint hport, jstring name)
{
  unsigned int id;
  const char * _name;
  struct hportInfo * port = &(hports[hport]);

  _name = (*env)->GetStringUTFChars(env, name, 0);
  id = gm_host_name_to_node_id(port->gm_port, _name);
  (*env)->ReleaseStringUTFChars(env, name, _name);

  if (id == GM_NO_SUCH_NODE_ID) {
    (*env)->ThrowNew(env, IOException, "no such node id");
    return 0;
  }

  return id;
}

JNIEXPORT jint JNICALL Java_xdev_gmdev_GM_gmGetNodeID__I_3B
  (JNIEnv * env, jclass clazz, jint hport, jbyteArray uid)
{
  struct hportInfo * port = &(hports[hport]);
  jbyte * uidp;
  gm_status_t status;
  unsigned int id;

  uidp = (*env)->GetPrimitiveArrayCritical(env, uid, 0);
  status = gm_unique_id_to_node_id(port->gm_port, (char *) uidp, &id);
  (*env)->ReleasePrimitiveArrayCritical(env, uid, uidp, JNI_ABORT);

  if (status != GM_SUCCESS) {
    sprintf(cbuf, "gm_unique_id_to_node_id() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
  }

  return (jint) id;
}

JNIEXPORT jbyteArray JNICALL Java_xdev_gmdev_GM_gmGetUID
  (JNIEnv * env, jclass clazz, jint hport, jint id)
{
  struct hportInfo * port = &(hports[hport]);
  jbyte * uidp;
  gm_status_t status;
  jbyteArray uid = (*env)->NewByteArray(env, 6);

  uidp = (*env)->GetPrimitiveArrayCritical(env, uid, 0);
  status = gm_node_id_to_unique_id(port->gm_port, (unsigned int) id, (char *) uidp);
  (*env)->ReleasePrimitiveArrayCritical(env, uid, uidp, JNI_COMMIT);

  if (status != GM_SUCCESS) {
    sprintf(cbuf, "gm_node_id_to_unique_id() failed, status=%d", status);
    (*env)->ThrowNew(env, IOException, cbuf);
    return NULL;
  }

  return uid;
}

JNIEXPORT void JNICALL Java_xdev_gmdev_GM_gmDump
  (JNIEnv * env, jclass clazz)
{
    printf("sticks=%lld\n", sticks);
    printf("rticks=%lld\n", rticks);
    printf("gticks=%lld\n", gticks);
    printf("scnt=%ld\n", scnt);
    printf("rcnt=%ld\n", rcnt);
    printf("maxQueueLen=%d\n", maxQueueLen);
    printf("unknownCnt=%d\n", unknownCnt);

    sticks = 0;
    rticks = 0;
    gticks = 0;
    scnt = 0;
    rcnt = 0;
    maxQueueLen = 0;
    unknownCnt = 0;
}

// on carla use:
//    gcc -Wall -W -Wno-unused -shared -fPIC -I /ToolMan/SysDep/jdk-1.3/include -I /ToolMan/SysDep/jdk-1.3/include/linux -I . -I /GM/include xdev_gmdev_GM.c -L /GM/lib -lgm -o libxdev_gmdev_GM.so

/*
 * 
 * Local Variables:
 *   compile-command: "gcc -Wall -W -Wno-unused -shared -fPIC -I /ToolMan/SysDep/jdk-1.3/include -I /ToolMan/SysDep/jdk-1.3/include/linux -I . -I /net/carla1/opt/myricom/gm-1.5pre2b-2.4.7-SMP/include xdev_gmdev_GM.c -L /net/carla1/opt/myricom/gm-1.5pre2b-2.4.7-SMP/lib -lgm -o ~/work/karmi/lib/libxdev_gmdev_GM.so"
 * End:
 * 
 */

