/*
 The MIT License

 Copyright (c) 2005 - 2008
   1. Distributed Systems Group, University of Portsmouth (2005)
   2. Aamir Shafi (2005 - 2008)
   3. Bryan Carpenter (2005 - 2008)
   4. Mark Baker (2005 - 2008)

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be included
 in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * File         : MPJProcessPrintStream.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.2 $
 * Updated      : $Date: 2005/08/14 20:37:43 $
 */

package runtime.daemon;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
/* 
 * We don't want Stdout class ...what we want to Outputhandler 
 * MPJOuputputStream and a proper constructor ....this is not 
 * good ... please sort it out ..
 */ 

public class MPJProcessPrintStream extends PrintStream {  

  static PrintStream oldStdout;
  static PrintStream oldStderr;
  static SocketChannel socketChannel;
  static ByteBuffer intBuffer = ByteBuffer.allocate(4);
  static ByteBuffer buffer = ByteBuffer.allocate(1000);

  MPJProcessPrintStream (PrintStream ps) {
    super(ps);
  }

  public static void start(SocketChannel sChannel, 
                           PrintStream i, PrintStream j)  
                                               throws IOException {

    oldStdout = System.out;
    oldStderr = System.err;
    socketChannel = sChannel;
    System.setOut(new MPJProcessPrintStream(i));
    System.setErr(new MPJProcessPrintStream(j)); 

  }

  public static void stop() { 

    System.setOut(oldStdout);
    System.setErr(oldStderr);
    try {
      buffer.clear();
      buffer.put("EXIT".getBytes(), 0, "EXIT".getBytes().length);
      buffer.flip();
      socketChannel.write(buffer);
      buffer.clear();
    }
    catch (Exception e) {
      e.printStackTrace();
    } 

  }

  public void write(int b) {

    try {
      intBuffer.putInt(b);
      intBuffer.flip();
      socketChannel.write(intBuffer);
      intBuffer.clear();
    }
    catch (Exception e) {
      e.printStackTrace();
      setError();
    } 

  }

  
  public void write(byte buf[], int off, int len) { 

    try {
      buffer.clear();
      buffer.put(buf, off, len);
      buffer.flip();
      socketChannel.write(buffer);
      buffer.clear();
    }
    catch (Exception e) {
      e.printStackTrace();
      setError();
    } 

  } 

}
