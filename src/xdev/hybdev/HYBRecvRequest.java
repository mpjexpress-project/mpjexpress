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
 * File         : HYBRecvRequest.java
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Thu Apr  9 12:22:15 BST 2004
 * Revision     : $Revision: 1.10 $
 * Updated      : $Date: 2005/12/21 13:18:18 $
 *    
 */

package xdev.hybdev;

import java.util.UUID;
import java.nio.channels.*;
import java.nio.*;
import mpjdev.*;
import mpjbuf.NIOBuffer ;
import xdev.*;

public class HYBRecvRequest
    extends HYBRequest {
  long sequenceNum ;	
  int code = -1;
  boolean readPending = false;
  int bytesRead = 0;

  HYBRecvRequest (ProcessID srcID, ProcessID dstID,
                 int tag, int context, mpjbuf.Buffer buf, boolean completed) {

    this.srcUUID = srcID.uuid() ;
    this.dstUUID = dstID.uuid() ;

    this.tag = tag;
    this.completed = completed;
    this.staticBuffer = ((NIOBuffer)buf.getStaticBuffer()).getBuffer() ;
    this.sBufSize = buf.getSize() ;
    this.buffer = buf;
    
    if(buf.getDynamicBuffer() != null && buf.getDynamicBuffer().length > 0) {
      this.dynamicBuffer = buf.getDynamicBuffer() ;
      this.dBufSize = buf.getDynamicBuffer().length ;
    }
    
    this.context = context ;
    this.status = new mpjdev.Status(srcID.uuid(), tag, -1);
		
  }
  
  int commMode = 0;

  int getCommMode() {
    return commMode;
  }

  void setCommMode(int commMode) {
    this.commMode = commMode;
  }

  public Status iwait() {
    if(alreadyCompleted) { 
      return status ; 	    
    }
    this.waitMe();

    /* this should be probably be done somewhere at the start of irecv
     * method */
    status.tag = this.tag;
    status.numEls = this.numEls;
    status.type = this.type;
    status.srcID = this.srcUUID;
    System.out.println ();
    complete(status);
    this.alreadyCompleted = true ; 
    return status;
  }

}
