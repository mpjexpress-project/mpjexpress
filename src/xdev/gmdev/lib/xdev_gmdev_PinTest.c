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
 * $Revision: 1.1.1.1 $
 * $Date: 2005/03/19 16:06:39 $
 */

#include <xdev_gmdev_PinTest.h>
#include <stdlib.h>

/* Inaccessible static: buffer */
/*
 * Class:     xdev_gmdev_PinTest
 * Method:    pinIt
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_PinTest_pinIt___3B
  (JNIEnv *env, jclass clazz, jbyteArray buffer)
{
    jboolean isCopy = 0;

    jbyte *_buffer = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    // jbyte *_buffer = (*env)->GetPrimitiveArrayCritical(env, buffer, &isCopy);

    if (! _buffer) {
	fprintf(stderr, "error: got no buffer.\n");
	exit(EXIT_FAILURE);
    }

    fprintf(stderr, "debug: buffer address: %p\n", _buffer);
    fprintf(stderr, "debug: isCody: %d\n", isCopy);

    _buffer[0] = 1;
}


/*
 * Class:     xdev_gmdev_PinTest
 * Method:    pinIt
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_xdev_gmdev_PinTest_pinIt__Ljava_lang_String_2
  (JNIEnv *env, jclass clazz, jstring str) 
{
    jboolean isCopy = 0;
    const char *_str = (*env)->GetStringUTFChars(env, str, &isCopy);

    if (! _str) {
	fprintf(stderr, "error: got no buffer.\n");
	exit(EXIT_FAILURE);
    }

    fprintf(stderr, "debug: buffer address: %p\n", _str);
    fprintf(stderr, "debug: isCody: %d\n", isCopy);
}
