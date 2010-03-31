/*
 The MIT License

 Copyright (c) 2005 - 2008
   1. Distributed Systems Group, University of Portsmouth (2005)
   2. Community Grids Laboratory, Indiana University (2005)
   3. Aamir Shafi (2005 - 2008)
   4. Bryan Carpenter (2005 - 2008)
   5. Mark Baker (2005 - 2008)

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
 * File         : Intracomm.java
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Fri Sep 10 12:22:15 BST 2004
 * Revision     : $Revision: 1.30 $
 * Updated      : $Date: 2006/10/20 17:24:47 $
 */

package mpi;

import mpjdev.*;
import mpjbuf.*;

public class Intracomm
    extends Comm {
  
  int bcast_tag =      35*1000 ;
  int nBARRIER_TAG =  (34*1000)+1 ; 	    
  int gatherTag =     (34*1000)+2 ; 
  int gathervTag =    (34*1000)+3 ; 
  int scatterTag =    (34*1000)+4 ;
  int scattervTag =   (34*1000)+5 ; 
  int allgatherTag =  (34*1000)+6 ;  
  int allgathervTag = (34*1000)+7 ;  
  int alltoallTag =   (34*1000)+8 ; 
  int alltoallvTag =  (34*1000)+9; 
  int reduceTag =     (34*1000)+10; 
  int allreduceTag =  (34*1000)+11; 
  int reducescatterTag = 
	              (34*1000)+12 ; 
  int scanTag =       (34*1000)+13 ; 

  Intracomm() {
  }

  int bCount = 1000;
  ProcTree procTree = null ; 

  /**
   * Constructor used to create an intracomm
   * @param mpjdev.Comm
   * The "obvious" way for allocating contexts is each process independently
   * increments a "next context" variable (a static variable if you like, but
   * local to the process).  Then the processes in the group do a collective
   * "maxval" of the candidates, and agree to accept the largest proposal.
   * This is returned as the result of the collective context allocation, and
   * all processes also store it in the static variable they use as a starting
   * point next time round.  This algorithm can also allocate several
   * consecutive contexts in a single phase, which is useful here.
   **/

  Intracomm(mpjdev.Comm mpjdevComm, mpjdev.Group group) throws MPIException {

    //MPI.logger.debug("--Intracomm--");
    this.mpjdevComm = mpjdevComm;
    this.group = new Group(group);

    procTree = new ProcTree();

    int root = 0 ;
    int index = Rank() ; 
    int extent = Size();
    int places = ProcTree.PROCTREE_A * index;

    //MPI.logger.debug(" init places <"+places+">");

    for (int i = 1; i <= ProcTree.PROCTREE_A; i++) {
      ++places;
      int ch = (ProcTree.PROCTREE_A * index) + i + root;
      //MPI.logger.debug("places "+places);
      ch %= extent;
      //MPI.logger.debug("ch "+ch);

      if (places < extent) {
        //MPI.logger.debug("ch <"+i+">"+"=<"+ch+">");
        //MPI.logger.debug("adding to the tree at index <"+(i-1) +">\n\n");
        procTree.child[i - 1] = ch;
        procTree.numChildren++;
      }
      else {
        //MPI.logger.debug("not adding to the tree");
      }
    }

    //MPI.logger.debug("procTree.numChildren <"+procTree.numChildren+">");

    if (index == root) {
      procTree.isRoot = true;
      //MPI.logger.debug("setting the root flag for root");
    }
    else {
      procTree.isRoot = false;
      int pr = (index - 1) / ProcTree.PROCTREE_A;
      procTree.parent = pr;
      //MPI.logger.debug("setting parent for non-root == procTree.parent "+
      //      procTree.parent);
    }

    procTree.root = root;
  }

  /**
   * Partition the group associated with this communicator and create
   * a new communicator within each subgroup.
   * <p>
   * <table>
   * <tr><td><tt> color    </tt></td><td> control of subset assignment </tr>
   * <tr><td><tt> key      </tt></td><td> control of rank assignment </tr>
   * <tr><td><em> returns: </em></td><td> new communicator </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_COMM_SPLIT</tt>.
   */
  public Intracomm Split(int color, int key) throws MPIException {
    Intracomm icomm = null;
    int[][] b = new int[this.group.Size()][3];
    int len = 0;
    int a[] = new int[2], ra[] = new int[2];
    a[0] = color;
    a[1] = key;
    ra[0] = 0;
    ra[1] = 0;
    b[len][0] = color;
    b[len][1] = key;
    b[len][2] = this.group.Rank();
    len++;

    int size = this.group.Size();
    int rank = this.group.Rank();
    int tag = 1110;

    mpi.Request[] reqs = new mpi.Request[size];

    /* send to all processes */
    for (int i = 0; i < size; i++) {
      if (i == rank)continue;
      reqs[i] = isend(a, 0, 2, MPI.INT, i, rank + tag + i, false);
    }

    /* now receive from all other processes */
    for (int i = 0; i < size; i++) {
      if (i == rank)continue;
      //MPI.out.print("p<"+rank+"> receving from <"+i+">");
      recv(ra, 0, 2, MPI.INT, i, (tag + i + rank), false);

      if (ra[0] == color) {
        b[len][0] = ra[0];
        b[len][1] = ra[1];
        b[len][2] = i;
        len++;
      }
    }

    /* complete send operation */
    for (int i = 0; i < size; i++) {
      if (i == rank)continue;
      reqs[i].Wait();
    }

    int keys[] = new int[len];
    for (int i = 0; i < len; i++) {
      keys[i] = b[i][1];
    }

    java.util.Arrays.sort(keys);
    int nids[] = new int[len];

    for (int i = 0; i < len; i++) {
      for (int j = 0; j < len; j++) {
        if (keys[i] == b[j][1])
          nids[i] = b[j][2];
      }
    }
    /*
        StringBuffer nbuffer = new StringBuffer();
        nbuffer.append("rank "+rank);
        for(int i=0 ; i<this.group.mpjdevGroup.ids.length ; i++) {
          nbuffer.append("ids["+i+"]="+this.group.mpjdevGroup.ids[i].uuid());
          nbuffer.append("ids["+i+"]="+this.group.mpjdevGroup.ids[i].rank());
        }
        if(rank == 0)
          System.out.println("\n "+nbuffer.toString());
     */
    try {
      mpjdev.Comm ncomm = mpjdevComm.create(nids);
      icomm = new Intracomm(ncomm, ncomm.group);
    }
    catch (Exception e) {
      throw new MPIException(e);	    
    }

    return icomm;
  }

  /**
   * Clone the communicator
   * This method will be called only by intracommunicator ....
   * changed the return value to Intracomm ...(instead of Object) ...
   */
  public Object clone() throws MPIException {
    return this.Create(this.group);
  }

  /**
   * Create a new communicator.
   * <p>
   * <table>
   * <tr><td><tt> group    </tt></td><td> group which is a subset of the
   *                                      group of this communicator </tr>
   * <tr><td><em> returns: </em></td><td> new communicator </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_COMM_CREATE</tt>.
   */
  public Intracomm Create(Group group) throws MPIException {
    mpjdev.Comm tcomm = mpjdevComm.create( group.mpjdevGroup );
    return (new Intracomm(tcomm, group.mpjdevGroup));
  }

  void nBarrier() throws MPIException {
/*
    int index = Rank();
    int root = 0;
    ProcTree procTree = new ProcTree();
    int extent = Size();
    int places = ProcTree.PROCTREE_A * index;

    for (int i = 1; i <= ProcTree.PROCTREE_A; i++) {
      ++places;
      int ch = (ProcTree.PROCTREE_A * index) + i + root;
      ch %= extent;

      if (places < extent) {
        procTree.child[i - 1] = ch;
        procTree.numChildren++;
      }
    }

    if (index == root) {
      procTree.isRoot = true;
    }
    else {
      procTree.isRoot = false;
      int pr = (index - 1) / ProcTree.PROCTREE_A;
      procTree.parent = pr;
    }

    procTree.root = root;
*/
    int offset =0;
    Datatype type = MPI.INT;
    int[] buf = new int[1];
    int count = 1;
    int btag = nBARRIER_TAG ;

    //------------------anti-bcast-------------------
    
    if (procTree.isRoot) {
      for (int i = 0; i < procTree.child.length; i++) {
        if (procTree.child[i] != -1)
          recv(buf, offset, count, type, procTree.child[i],
               btag - procTree.child[i], false);
      }
    }
    else {
      if (procTree.parent == -1) {
        System.out.println("non root's node parent doesn't exist");
      }

      for (int i = 0; i < procTree.child.length; i++) {
        if (procTree.child[i] != -1)
          recv(buf, offset, count, type, procTree.child[i],
               btag - procTree.child[i], false);
      }

      send(buf, offset, count, type, procTree.parent, 
		      btag - Rank(), false);
    }
    
    //------------------bcast-------------------

    if (procTree.isRoot) {
      for (int i = 0; i < procTree.child.length; i++) {
        if (procTree.child[i] != -1)
          send(buf, offset, count, type, procTree.child[i],
               btag - procTree.child[i], false);
      }
    }
    else {
      if (procTree.parent == -1) {
        System.out.println("non root's node parent doesn't exist");
      }

      recv(buf, offset, count, type, procTree.parent, 
		      btag - Rank(), false);

      for (int i = 0; i < procTree.child.length; i++) {
        if (procTree.child[i] != -1)
          send(buf, offset, count, type, procTree.child[i],
               btag - procTree.child[i], false);
      }
    }

    

  }

  void newBarrier() throws MPIException {

    if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
      MPI.logger.debug("--new Barrier starts--");
    }
    int size = Size();
    int rank = Rank();
    Request req = null;
    int stuff[] = new int[1];
    int y = (int) Math.pow(2d, Math.floor(Math.log(size) / Math.log(2)));

    if (rank >= y) {

      try {
        req = isend(stuff, 0, 1, MPI.INT, rank - y, 
		      ( (999 + rank - y) * bCount), false);
        recv(stuff, 0, 1, MPI.INT, rank - y, ( (999 + rank) * bCount), false);
      }
      catch (Exception e) {
	      e.printStackTrace();
        return;	      
      }
      
      req.Wait();

    }
    else {

      if ( (size - y) > rank) {

        try {
          recv(stuff, 0, 1, MPI.INT, rank + y, (999 + rank) * bCount, false);
        }
        catch (Exception e) {
	      e.printStackTrace();
          return;		
        }
      }

      int round = -1;
      int peer = 0;

      do {
        round = round + 1;
        peer = rank ^ (int) Math.pow(2d, round);
        try {
          req = isend(stuff, 0, 1, MPI.INT, peer, (999 + peer) * bCount, false);
          recv(stuff, 0, 1, MPI.INT, peer, (999 + rank) * bCount, false);
        }
        catch (Exception e) {
	      e.printStackTrace();
          return;		
        }

      }
      while (round != ( (int) (Math.log(y) / Math.log(2)) - 1));

      if ( (size - y) > rank) {

        try {	      
          req = isend(stuff, 0, 1, MPI.INT, rank + y, 
			  (999 + rank + y) * bCount, false);
	}
	catch(Exception e) {
	      e.printStackTrace();
          return;
	}

      }

    }

    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--new Barrier ends--");
  }

  private void exoticBarrier() throws MPIException { 
    int size, rank, src, dst, mask ; 

    size = Size() ; 
    rank = Rank() ; 
    byte[] barrierMsg = new byte[1] ; 
    mask = 0x1 ; 
  
    while(mask < size) { 
      dst = (rank + mask) % size;
      src = (rank - mask + size) % size;

      sendrecv(barrierMsg, 0, 1, MPI.BYTE, dst, nBARRIER_TAG, 
               barrierMsg, 0, 1, MPI.BYTE, src, nBARRIER_TAG) ; 
      mask <<= 1;
    } 
      
  }

  /**
   * Broadcast a message from the process with rank <tt>root</tt>
   * to all processes of the group.
   * <p>
   * <table>
   * <tr><td><tt> buf      </tt></td><td> buffer array </tr>
   * <tr><td><tt> offset   </tt></td><td> initial offset in buffer </tr>
   * <tr><td><tt> count    </tt></td><td> number of items in buffer </tr>
   * <tr><td><tt> datatype </tt></td><td> datatype of each item in
   *                                      buffer </tr>
  /**
   * A call to <tt>Barrier</tt> blocks the caller until all process
   * in the group have called it.
   * <p>
   * Java binding of the MPI operation <tt>MPI_BARRIER</tt>.
   */
  public void Barrier() throws MPIException {
    if(Size() == 1) { 
      return ;    	    
    }

    if(EXOTIC_BARRIER) { 
      exoticBarrier() ; 
      return ; 
    } 

    nBarrier();
    //newBarrier();
/*
    MPI.logger.debug("--Barrier starts--");
    bCount++;
    int size = Size();
    int rank = Rank();
    Request req = null;
    int stuff[] = new int[1];
    int y = (int) Math.pow(2d, Math.floor(Math.log(size) / Math.log(2)));
    MPI.logger.debug("y " + y);

    if (rank >= y) {
      MPI.logger.debug("Group B");
      MPI.logger.debug(" pro<" + rank + "> sending a message with tag<" +
                ( (999 + rank - y) * bCount) + "> to pro<" + (rank - y));
      req = isend(stuff, 0, 1, MPI.INT, rank - y, 
		      ( (999 + rank - y) * bCount), false);
      MPI.logger.debug(" pro<" + rank + "> receiving a message with tag<" +
                ( (999 + rank) * bCount) + "> from pro<" + (rank - y));
      try {
        recv(stuff, 0, 1, MPI.INT, rank - y, ( (999 + rank) * bCount), false);
      }
      catch (Exception e) {
        throw new MPIException( e );	      
      }
      
      req.Wait();
    }
    else {
      MPI.logger.debug("Group A");
      if ( (size - y) > rank) {
        MPI.logger.debug("this process got partners in Group B");
        MPI.logger.debug(" pro<" + rank + "> sending a message with tag<" +
                  ( (999 + rank) * bCount) + "> to pro<" + (rank + y));
        try {
          recv(stuff, 0, 1, MPI.INT, rank + y, (999 + rank) * bCount, false);
        }
        catch (Exception e) {
          throw new MPIException( e );	      
        }
      }

      int round = -1;
      int peer = 0;

      do {
        round = round + 1;
        MPI.logger.debug("round " + round);
        MPI.logger.debug("round limit <" + (Math.log(y) / Math.log(2)));
        peer = rank ^ (int) Math.pow(2d, round);
        MPI.logger.debug("peer " + peer);
        MPI.logger.debug(" pro<" + rank + "> sending a message with tag<" +
                  ( (999 + peer) * bCount) + "> to pro<" + peer + ">");
        req = isend(stuff, 0, 1, MPI.INT, peer, (999 + peer) * bCount, false);
        MPI.logger.debug(" pro<" + rank + "> receiving a message with tag<" +
                  ( (999 + rank) * bCount) + "> from pro<" + peer + ">");
        try {
          recv(stuff, 0, 1, MPI.INT, peer, (999 + rank) * bCount, false);
        }
        catch (Exception e) {
          throw new MPIException( e );	      
        }
        
        req.Wait();

      }
      while (round != ( (int) (Math.log(y) / Math.log(2)) - 1));

      if ( (size - y) > rank) {
        MPI.logger.debug(" pro<" + rank + "> sending a message with tag<" +
                ( (999 + rank + y) * bCount) + "> to pro<" + (rank + y) + ">");
        req = isend(stuff, 0, 1, MPI.INT, rank + y, 
			(999 + rank + y) * bCount, false);
        req.Wait();
      }

    }

    if (bCount == Integer.MAX_VALUE - 1)
      bCount = 1000;

    MPI.logger.debug("--Barrier ends--");
*/    
  }

  /**
   * Broadcast a message from the process with rank <tt>root</tt>
   * to all processes of the group.
   * <p>
   * <table>
   * <tr><td><tt> buf      </tt></td><td> buffer array </tr>
   * <tr><td><tt> offset   </tt></td><td> initial offset in buffer </tr>
   * <tr><td><tt> count    </tt></td><td> number of items in buffer </tr>
   * <tr><td><tt> datatype </tt></td><td> datatype of each item in
   *                                      buffer </tr>
   * <tr><td><tt> root     </tt></td><td> rank of broadcast root </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_BCST</tt>.
   */

  public void Bcast(Object buf,
                    int offset,
                    int count,
                    Datatype type,
                    int root) throws MPIException {

    int index = Rank();

    if (root != 0) {
      if (root == index)
        send(buf, offset, count, type, 0, bcast_tag, false);

      if (index == 0)
        recv(buf, offset, count, type, root, bcast_tag - Rank(), false);
    }

    root = 0;
/*
    ProcTree procTree = new ProcTree();
    int extent = Size();
    int places = ProcTree.PROCTREE_A * index;

    //MPI.logger.debug(" init places <"+places+">");

    for (int i = 1; i <= ProcTree.PROCTREE_A; i++) {
      ++places;
      int ch = (ProcTree.PROCTREE_A * index) + i + root;
      //MPI.logger.debug("places "+places);
      ch %= extent;
      //MPI.logger.debug("ch "+ch);

      if (places < extent) {
        //MPI.logger.debug("ch <"+i+">"+"=<"+ch+">");
        //MPI.logger.debug("adding to the tree at index <"+(i-1) +">\n\n");
        procTree.child[i - 1] = ch;
        procTree.numChildren++;
      }
      else {
        //MPI.logger.debug("not adding to the tree");
      }
    }

    //MPI.logger.debug("procTree.numChildren <"+procTree.numChildren+">");

    if (index == root) {
      procTree.isRoot = true;
      //MPI.logger.debug("setting the root flag for root");
    }
    else {
      procTree.isRoot = false;
      int pr = (index - 1) / ProcTree.PROCTREE_A;
      procTree.parent = pr;
      //MPI.logger.debug("setting parent for non-root == procTree.parent "+
      //      procTree.parent);
    }

    procTree.root = root;

    //for (int i = 0; i < procTree.PROCTREE_A; i++) {
      //MPI.out.print(" child["+i+"]=>"+procTree.child[i]);
    //}

    //MPI.logger.debug("  ------- End --------");
    //MPI.logger.debug("---Bcast---");
*/

    if (procTree.isRoot) {
      //MPI.logger.debug("This process is root");
      for (int i = 0; i < procTree.child.length; i++) {
        //MPI.logger.debug("sending to children <"+i+">");
        //MPI.logger.debug("procTree.child[i] <"+procTree.child[i]+">");
        if (procTree.child[i] != -1)
          send(buf, offset, count, type, procTree.child[i],
               bcast_tag - procTree.child[i], false);
        //MPI.logger.debug("Sent");
      }
    }
    else {
      if (procTree.parent == -1) {
        System.out.println("non root's node parent doesn't exist");
      }
      //MPI.logger.debug("This process is not root");
      //MPI.logger.debug("receiving from parent ");
      //MPI.logger.debug("procTree.parent <"+procTree.parent+">");
      recv(buf, offset, count, type, procTree.parent, bcast_tag - Rank(), false);

      for (int i = 0; i < procTree.child.length; i++) {
        //MPI.logger.debug("sending to children <"+i+">");
        //MPI.logger.debug("procTree.child[i] <"+procTree.child[i]+">");
        if (procTree.child[i] != -1)
          send(buf, offset, count, type, procTree.child[i],
               bcast_tag - procTree.child[i], false);
        //MPI.logger.debug("Sent");
      }
    }

    /* 0(N) algorithm ...
         if(root == Rank()) {
     for(int i=0;i<Size();i++) {
       if(i == Rank())
         continue;

       send(buf,offset,count,type,i,bcast_tag-i, false);
       //MPI.logger.debug("root "+root+" sent msg to "+i);
     }
         }else {
     recv(buf,offset,count,type,root,bcast_tag-Rank(), false);
     //MPI.logger.debug("process "+Rank()+" recvd msg from"+root);
         }
     */

    //MPI.logger.debug("--Bcast ends--");
    if(bcast_tag == 65535) {
     bcast_tag = 35*1000 ;  	    
    }
    bcast_tag++ ;
  }


  /**
   * Each process sends the contents of its send buffer to the
   * root process.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items to send </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcount  </tt></td><td> number of items to receive </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * <tr><td><tt> root       </tt></td><td> rank of receiving process </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_GATHER</tt>.
   */
  public void Gather(Object sendbuf,
                     int sendoffset,
                     int sendcount,
                     Datatype sendtype,
                     Object recvbuf,
                     int recvoffset,
                     int recvcount,
                     Datatype recvtype,
                     int root) throws MPIException {

    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--Gather--");
    int rcount = -1, roffset = -1, soffset = -1, scount = -1;
    Request req = null;
    if (root != Rank()) {
      req = isend(sendbuf, sendoffset, sendcount, sendtype, root, 
		      gatherTag+Rank(), false);
      if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
        MPI.logger.debug("ssent to " + root);
      }
    }
    else {
      scount = sendcount;
      soffset = sendoffset;
    }

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (i != root) {
          if(MPI.DEBUG && MPI.logger.isDebugEnabled())
            MPI.logger.debug("root " + root + " getting from " + i);
          recv(recvbuf, recvoffset, recvcount, recvtype, i, 
			  gatherTag + i, false);
          if(MPI.DEBUG && MPI.logger.isDebugEnabled())
            MPI.logger.debug("root " + root + " got from " + i);
        }
        else {
          roffset = recvoffset;
          rcount = recvcount;
        }

        recvoffset += recvcount;
      }
    }
    if (rcount != scount) {
      System.out.println(" scount != rcount, should not happen");
      System.out.println(" this is Reduce method");
    }

    if (root == Rank()) {
      System.arraycopy(sendbuf, soffset, recvbuf,
                       roffset, rcount);
    }
    else {
      req.Wait();
    }
    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--Gather ends--");
  }


  /**
   * Extends functionality of <tt>Gather</tt> by allowing varying
   * counts of data from each process.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items to send </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcounts </tt></td><td> number of elements received from
   *                                        each process </tr>
   * <tr><td><tt> displs     </tt></td><td> displacements at which to place
   *                                        incoming data </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * <tr><td><tt> root       </tt></td><td> rank of receiving process </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_GATHERV</tt>.
   * <p>
   * The sizes of arrays <tt>recvcounts</tt> and <tt>displs</tt> should be the
   * size of the group.  Entry <em>i</em> of <tt>displs</tt> specifies the
   * displacement relative to element <tt>recvoffset</tt> of <tt>recvbuf</tt>
   * at which to place incoming data.
   */
  public void Gatherv(Object sendbuf,
                      int sendoffset,
                      int sendcount,
                      Datatype sendtype,
                      Object recvbuf,
                      int recvoffset,
                      int[] recvcount,
                      int[] displs,
                      Datatype recvtype,
                      int root) throws MPIException {

    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--Gatherv--");
    int rcount = -1, roffset = -1;
    Request req = null;
    if (root != Rank()) {
      req = isend(sendbuf, sendoffset, sendcount, sendtype, root, 
		      gathervTag+Rank(), false);
      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("ssent to " + root);
    }

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (i != root) {
          if(MPI.DEBUG && MPI.logger.isDebugEnabled())
             MPI.logger.debug("root " + root + " getting from " + i);
          recv(recvbuf, recvoffset + displs[i], recvcount[i], recvtype, i,
               gathervTag+i, false);
          if(MPI.DEBUG && MPI.logger.isDebugEnabled())
            MPI.logger.debug("root " + root + " got from " + i);
        }
        else {
          roffset = recvoffset + displs[i];
          rcount = recvcount[i];
        }
      }
    }

    if (root == Rank()) {
      if (rcount != sendcount) {
        System.out.println(" scount != rcount, should not happen");
        System.out.println(" this is Reduce method");
      }

      System.arraycopy(sendbuf, sendoffset, recvbuf,
                       roffset, rcount);
    }
    else {
      req.Wait();
    }

    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--Gatherv ends--");
  }


  /**
   * Inverse of the operation <tt>Gather</tt>.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items to send </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcount  </tt></td><td> number of items to receive </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * <tr><td><tt> root       </tt></td><td> rank of sending process </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_SCATTER</tt>.
   */
  

  public void Scatter(Object sendbuf,
                      int sendoffset,
                      int sendcount,
                      Datatype sendtype,
                      Object recvbuf,
                      int recvoffset,
                      int recvcount,
                      Datatype recvtype,
                      int root) throws MPIException {

    //MPI.logger.debug("--Scatter--");
    Request[] reqs = new Request[Size()];
    int soffset = -1, scount = -1;

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (root != i)
          reqs[i] = isend(sendbuf, sendoffset, sendcount, sendtype, i, 
			  scatterTag+i, false);
                          
        else {
          soffset = sendoffset;
          scount = sendcount;
        }
        sendoffset += sendcount;
      }
    }

    if (root != Rank())
      recv(recvbuf, recvoffset, recvcount, recvtype, root, 
		      scatterTag+Rank(), false);
    else {
      if (scount != recvcount) {
        System.out.println(" scount shuld be equal to recvcount");
      }
      System.arraycopy(sendbuf, soffset, recvbuf, recvoffset,
                       recvcount);
    }

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (i != root)
          reqs[i].Wait();
      }
    }

    //MPI.logger.debug("--Scatter ends--");
  }


  /**
   * Inverse of the operation <tt>Gatherv</tt>.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcounts </tt></td><td> number of items sent to each
   *                                        process </tr>
   * <tr><td><tt> displs     </tt></td><td> displacements from which to take
   *                                        outgoing data </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcount  </tt></td><td> number of items to receive </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * <tr><td><tt> root       </tt></td><td> rank of sending process </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_SCATTERV</tt>.
   */
  public void Scatterv(Object sendbuf,
                       int sendoffset,
                       int[] sendcount,
                       int[] displs,
                       Datatype sendtype,
                       Object recvbuf,
                       int recvoffset,
                       int recvcount,
                       Datatype recvtype,
                       int root) throws MPIException {

    //MPI.logger.debug("--Scatter--");
    Request[] reqs = new Request[Size()];
    int soffset = -1, scount = -1;

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (root != i)
          reqs[i] = isend(sendbuf, sendoffset + displs[i],
                          sendcount[i], sendtype, i, scattervTag+i, false);
        else {
          soffset = sendoffset + displs[i];
          scount = sendcount[i];
        }
      }
    }

    if (root != Rank())
      recv(recvbuf, recvoffset, recvcount, recvtype, root, 
		      scattervTag+Rank(), false);
    else {
      if (scount != recvcount) {
        System.out.println(" scount shuld be equal to recvcount");
      }
      System.arraycopy(sendbuf, soffset, recvbuf, recvoffset,
                       recvcount);
    }

    if (root == Rank()) {
      for (int i = 0; i < Size(); i++) {
        if (i != root)
          reqs[i].Wait();
      }
    }

    //MPI.logger.debug("--Scatter ends--");
  }


  /**
   * Similar to <tt>Gather</tt>, but all processes receive the result.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items to send </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcount  </tt></td><td> number of items to receive </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_ALLGATHER</tt>.
   */

  public void Allgather(Object sendbuf,
                        int sendoffset,
                        int sendcount,
                        Datatype sendtype,
                        Object recvbuf,
                        int recvoffset,
                        int recvcount,
                        Datatype recvtype) throws MPIException {

    if(EXOTIC_ALLGATHER) { 

      int me = Rank() ; 
      int size = Size() ; 
   
      int pof2 = 1;

      while (pof2 < size)
        pof2 *= 2;

      if (pof2 == size)
        pof2 = 1;
      else
        pof2 = 0;

      System.arraycopy(sendbuf, sendoffset, recvbuf, recvcount*me, 
                                recvcount) ; 

      int curr_cnt = recvcount;

      int mask = 0x1;
      int i = 0, dst ; 
      int dst_tree_root, my_tree_root ; 
      int send_offset, recv_offset ; 
      int last_recv_cnt ; 
       
      while(mask < size) { 

        dst = me ^ mask ; 

        dst_tree_root = dst >> i;
        dst_tree_root <<= i;

        my_tree_root = me >> i;
        my_tree_root <<= i;     

        send_offset = my_tree_root * recvcount ; 
        recv_offset = dst_tree_root * recvcount ;

        Status status = sendrecv(recvbuf,send_offset, curr_cnt, sendtype, dst, 
                         allgatherTag, recvbuf, 
                         recv_offset, recvcount*mask, recvtype, dst,
                         allgatherTag) ; 

        last_recv_cnt = status.Get_count(recvtype); 
        curr_cnt += last_recv_cnt ; 

        mask <<= 1 ; 
        i++ ; 
      
      } //end while 

    } // end if(true)

    else { 
      //MPI.logger.debug("--Allgather--");
      Request req[] = new Request[Size()];
      int rcount = -1, roffset = -1;

      for (int i = 0; i < Size(); i++) {
        if (i != Rank())
          req[i] = isend(sendbuf, sendoffset, sendcount, sendtype, i,
                         allgatherTag + i + Rank(),
                         false);
      }

      for (int i = 0; i < Size(); i++) {
        if (i != Rank())
          recv(recvbuf, recvoffset, recvcount, recvtype, i, 
	  		allgatherTag + Rank() + i, false);
        else {
          rcount = recvcount;
          roffset = recvoffset;
        }

        recvoffset += recvcount;
      }

      //if (rcount != sendcount) {
      //  System.out.println("rcount not equal to sendcount");
      //  System.out.println("this is not possible, should not happen!");
      //}

      System.arraycopy(sendbuf, sendoffset, recvbuf, roffset, rcount);

      for (int i = 0; i < Size(); i++) {
        if (i != Rank())
          req[i].Wait();
      }
    }   

    //MPI.logger.debug("--Allgather Ends--");
  }

  static boolean EXOTIC_ALLGATHER = false ; 
  static boolean EXOTIC_ALLGATHERV = false ; 
  static boolean EXOTIC_BARRIER = false ; 

  /**
   * Similar to <tt>Gatherv</tt>, but all processes receive the result.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items to send </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcounts </tt></td><td> number of elements received from
   *                                        each process </tr>
   * <tr><td><tt> displs     </tt></td><td> displacements at which to place
   *                                        incoming data </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_ALLGATHERV</tt>.
   */
  public void Allgatherv(Object sendbuf,
                         int sendoffset,
                         int sendcount,
                         Datatype sendtype,
                         Object recvbuf,
                         int recvoffset,
                         int[] recvcount,
                         int[] displs,
                         Datatype recvtype) throws MPIException {

    if(EXOTIC_ALLGATHERV) { 
      int total_count=0 ;  
      Object tmp_buf = null ; 
      int me = Rank() ; 
      int size = Size() ; 
   
      for(int i=0 ; i<recvcount.length ; i++) { 
        total_count += recvcount[i] ;  
      }  

      if(total_count == 0) { 
        return ; 
      } 

      int pof2 = 1;

      while (pof2 < size)
        pof2 *= 2;

      if (pof2 == size)
        pof2 = 1;
      else
        pof2 = 0;

      if(sendtype == MPI.LONG) { 
        tmp_buf = new long[total_count] ; 
        tmp_buf = (long[]) tmp_buf ; 
      } 
      else if(sendtype == MPI.INT) { 
        tmp_buf = new  int[total_count];
        tmp_buf = (int[]) tmp_buf ; 
      } 

      int position = 0;            

      for (int i=0; i<me ; i++) { 
        position += recvcount[i];
      } 

      System.arraycopy(sendbuf, sendoffset, tmp_buf, position, 
                                recvcount[me] ) ; 

      int curr_cnt = recvcount[me];

      int mask = 0x1;
      int i = 0, dst ; 
      int dst_tree_root, my_tree_root ; 
      int send_offset, recv_offset ; 
      int last_recv_cnt ; 
       
      while(mask < size) { 

        dst = me ^ mask ; 

        dst_tree_root = dst >> i;
        dst_tree_root <<= i;

        my_tree_root = me >> i;
        my_tree_root <<= i;     

        send_offset = 0;

        for (int j=0; j<my_tree_root; j++)
          send_offset += recvcount[j];

        recv_offset = 0 ; 
    
        for (int j=0; j<dst_tree_root; j++)
          recv_offset += recvcount[j];

        Status status = sendrecv(tmp_buf,send_offset, curr_cnt, sendtype, dst, 
                         allgathervTag, tmp_buf, 
                         recv_offset, total_count, recvtype, dst,
                         allgathervTag) ; 

        last_recv_cnt = status.Get_count(recvtype); 
        curr_cnt += last_recv_cnt ; 

        mask <<= 1 ; 
        i++ ; 
      
      } //end while 

      position = 0 ; 
      for(int j=0 ; j<size ;j++) { 
       
        System.arraycopy(tmp_buf, position, 
                         recvbuf, recvoffset+displs[j], recvcount[j]) ; 
        position += recvcount[j] ;  
       
      } 
  
    } // end if(true)

    else { 

    //MPI.logger.debug("--Allgatherv--");
    Request req[] = new Request[Size()];
    Request rreq[] = new Request[Size()];
    int rcount = -1, roffset = -1, i=0;

    for (i = 0; i < Size(); i++) {
      if (i != Rank())
        req[i] = isend(sendbuf, sendoffset, sendcount, sendtype, i,
                       allgathervTag + i + Rank(),
                       false);
    }

    for (i = 0; i < Size(); i++) {
      if (i != Rank())
        rreq[i] = irecv(recvbuf, recvoffset + displs[i], recvcount[i], 
                        recvtype, i,
                        allgathervTag + i + Rank(), false);
      else {
        rcount = recvcount[i];
        roffset = recvoffset + displs[i];
      }

    }

    //if (rcount != sendcount) {
    //  System.out.println("rcount not equal to sendcount");
    //  System.out.println("this is not possible, should not happen!");
    //}

    System.arraycopy(sendbuf, sendoffset, recvbuf, roffset, rcount);

    for (i = 0; i < Size(); i++) {
      if (i != Rank())
        req[i].Wait();
    }

    for (i = 0; i < Size(); i++) {
      if (i != Rank())
        rreq[i].Wait();
    }
    //MPI.logger.debug("--Allgather Ends--");

    } 
  }


  /**
   * Extension of <tt>Allgather</tt> to the case where each process sends
   * distinct data to each of the receivers.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcount  </tt></td><td> number of items sent to each
   *                                        process </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype send buffer items </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcount  </tt></td><td> number of items received from any
   *                                        process
   * <tr><td><tt> recvtype   </tt></td><td> datatype of receive buffer
   *                                        items </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_ALLTOALL</tt>.
   */


  public void Alltoall(Object sendbuf,
                       int sendoffset,
                       int sendcount,
                       Datatype sendtype,
                       Object recvbuf,
                       int recvoffset,
                       int recvcount,
                       Datatype recvtype) throws MPIException {

    if(MPI.DEBUG && MPI.logger.isDebugEnabled())
      MPI.logger.debug("--All to all--");
    Request req[] = new Request[Size()];
    int soffset = -1, scount = -1, roffset = -1, rcount = -1;

    for (int i = 0; i < Size(); i++) {

      if (i != Rank()) {
        if(MPI.DEBUG && MPI.logger.isDebugEnabled())
          MPI.logger.debug("proc <" + Rank() + 
			  ">-sending message to proc<" + i + ">");
        req[i] = isend(sendbuf, sendoffset, sendcount, sendtype, i,
                       alltoallTag * (76 + (i + 1)),
                       false);
      }
      else {
        soffset = sendoffset;
        scount = sendcount;
      }

      if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
        MPI.logger.debug("soffset" + soffset);
        MPI.logger.debug("scount" + scount);
        MPI.logger.debug("process " + Rank() + "sent to process " + i);
      }
      sendoffset += sendcount;
    }

    for (int i = 0; i < Size(); i++) {

      if (i != Rank()) {
        if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
          MPI.logger.debug("proc <" + Rank() + 
			  "> recving from proc<" + i + ">");
	}
        recv(recvbuf, recvoffset, recvcount, recvtype, i,
             alltoallTag * (76 + (Rank() + 1)), false);
        if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
          MPI.logger.debug("recvoffset " + recvoffset);
          MPI.logger.debug("recvcount " + recvcount);
          MPI.logger.debug("process " + Rank() + "recvd from process " + i);
	}
      }
      else {
        roffset = recvoffset;
        rcount = recvcount;
      }
      recvoffset += recvcount;
    }

    //copy from sendbuffer to recvbuffer ...
    if (scount != rcount) {
      System.out.println("Alltoall, rcount not equal to scount");
      System.out.println("this can never happen ...");
    }

    if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
      MPI.logger.debug("copying the message from sendbuffer to recvbuffer");
    }
    System.arraycopy(sendbuf, soffset, recvbuf,
                     roffset, rcount);

    for (int i = 0; i < Size(); i++) {
      if (i == Rank())
        continue;

      req[i].Wait();
    }
  }


  /**
   * Adds flexibility to <tt>Alltoall</tt>: location of data for send is
   * specified by <tt>sdispls</tt> and location to place data on receive
   * side is specified by <tt>rdispls</tt>.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> sendcounts </tt></td><td> number of items sent to each
   *                                        process </tr>
   * <tr><td><tt> sdispls    </tt></td><td> displacements from which to take
   *                                        outgoing data </tr>
   * <tr><td><tt> sendtype   </tt></td><td> datatype send buffer items </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcounts </tt></td><td> number of elements received from
   *                                        each process
   * <tr><td><tt> rdispls    </tt></td><td> displacements at which to place
   *                                        incoming data </tr>
   * <tr><td><tt> recvtype   </tt></td><td> datatype of each item in receive
   *                                        buffer </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_ALLTOALLV</tt>.
   */

  public void Alltoallv(Object sendbuf,
                        int sendoffset,
                        int[] sendcount,
                        int[] sdispls,
                        Datatype sendtype,
                        Object recvbuf,
                        int recvoffset,
                        int[] recvcount,
                        int[] rdispls,
                        Datatype recvtype) throws MPIException {

    //MPI.logger.debug("--All to all--");
    Request req[] = new Request[Size()];
    int soffset = -1, scount = -1, roffset = -1, rcount = -1;

    for (int i = 0; i < Size(); i++) {

      if (i != Rank()) {
        req[i] = isend(sendbuf, sendoffset + sdispls[i], sendcount[i],
                       sendtype, i, alltoallvTag, false);
      }
      else {
        soffset = sendoffset + sdispls[i];
        scount = sendcount[i];
      }

      //MPI.logger.debug("sendoffset "+sendoffset);
      //MPI.logger.debug("sendcount "+sendcount);
      //MPI.logger.debug("process "+Rank()+"sent to process "+i);
    }

    for (int i = 0; i < Size(); i++) {

      //MPI.logger.debug("process "+Rank()+"recving from process "+i);
      if (i != Rank()) {
        recv(recvbuf, recvoffset + rdispls[i], recvcount[i], recvtype, i, 
			alltoallvTag, false);
      }
      else {
        roffset = recvoffset + rdispls[i];
        rcount = recvcount[i];
      }
      //MPI.logger.debug("recvoffset "+recvoffset);
      //MPI.logger.debug("recvcount "+recvcount);
      //MPI.logger.debug("process "+Rank()+"recvd from process "+i);
    }

    //copy from sendbuffer to recvbuffer ...
    if (scount != rcount) {
      System.out.println("Alltoall, rcount not equal to scount");
      System.out.println("this can never happen ...");
    }

    System.arraycopy(sendbuf, soffset, recvbuf,
                     roffset, rcount);

    for (int i = 0; i < Size(); i++) {
      if (i == Rank())
        continue;

      req[i].Wait();
    }

  }


  /**
   * Combine elements in input buffer of each process using the reduce
   * operation, and return the combined value in the output buffer of the
   * root process.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> count      </tt></td><td> number of items in send buffer </tr>
   * <tr><td><tt> datatype   </tt></td><td> data type of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> op         </tt></td><td> reduce operation </tr>
   * <tr><td><tt> root       </tt></td><td> rank of root process </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_REDUCE</tt>.
   * <p>
   * The predefined operations are available in Java as <tt>MPI.MAX</tt>,
   * <tt>MPI.MIN</tt>, <tt>MPI.SUM</tt>, <tt>MPI.PROD</tt>, <tt>MPI.LAND</tt>,
   * <tt>MPI.BAND</tt>, <tt>MPI.LOR</tt>, <tt>MPI.BOR</tt>, <tt>MPI.LXOR</tt>,
   * <tt>MPI.BXOR</tt>, <tt>MPI.MINLOC</tt> and <tt>MPI.MAXLOC</tt>.
   */

  public void Reduce(Object sendbuf, int sendoffset,
                     Object recvbuf, int recvoffset, int count,
                     Datatype datatype, Op op, int root) throws MPIException {

    if(op.worker == null) {

      Request req = null;
      //BUG:
      //Object tmpbuf = sendbuf;
      Object tmpbuf = createTemporaryBuffer(datatype, count) ; 

      if(root != Rank()) 
        req = isend(sendbuf, sendoffset, count, datatype, root, 
			reduceTag, false);
      else 
        System.arraycopy(sendbuf, sendoffset, tmpbuf, sendoffset,
			count*datatype.size);
        
      if (Rank() == root) {
        for (int i = 0; i < Size(); i++) {
          if (i == Rank()) {
            continue;
          }
          recv(recvbuf, recvoffset, count, datatype, i, reduceTag, false);
          op.funct.Call(recvbuf, recvoffset, tmpbuf, 
                        sendoffset, count, datatype);
        }
      }

      if (root != Rank())
        req.Wait();

      System.arraycopy(tmpbuf, sendoffset, recvbuf, recvoffset, 
		      count*datatype.size);
      return;

    }//end if user_defined datatypes.


    /* Code for pre-defined operations */	  
    Op op2 = op.worker.getWorker(datatype);
    op2.createInitialBuffer(sendbuf, sendoffset, count); 
    Request req = null;

    if (root != Rank()) 
      req = isend(sendbuf, sendoffset, count, datatype, root, 
		      reduceTag, false);
    //else
      //op2.perform(sendbuf, sendoffset, count);

    if (Rank() == root) {
      for (int i = 0; i < Size(); i++) {
        if (i == Rank()) {
          continue;
        }
        recv(recvbuf, recvoffset, count, datatype, i, reduceTag, false);
        op2.perform(recvbuf, recvoffset, count);
      }
    }

    op2.getResultant(recvbuf, recvoffset, count);

    if (root != Rank())
      req.Wait();

  }

  static boolean EXOTIC_ALLREDUCE = false ; 


  /**
   * Same as <tt>reduce</tt> except that the result appears in receive
   * buffer of all process in the group.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> count      </tt></td><td> number of items in send buffer </tr>
   * <tr><td><tt> datatype   </tt></td><td> data type of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> op         </tt></td><td> reduce operation </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_ALLREDUCE</tt>.
   */

  Object createTemporaryBuffer(Datatype datatype, int count) { 

    Object tempBuffer = null ; 

    switch (datatype.baseType) {

      case Datatype.UNDEFINED:
        System.out.println("Intracomm.createTemporaryBuffer() - "+
                           "UNDEFINED datatype"); 
        break;

      case Datatype.NULL:
        System.out.println("Intracomm.createTemporaryBuffer() - "+
                           "NULL datatype"); 
        break;

      case Datatype.BYTE:
        tempBuffer = new byte[count*datatype.Size()] ;
        break;

      case Datatype.CHAR:
        tempBuffer = new char[count*datatype.Size()] ;
        break;

      case Datatype.SHORT:
        tempBuffer = new short[count*datatype.Size()] ;
        break;

      case Datatype.BOOLEAN:
        tempBuffer = new byte[count*datatype.Size()] ;
        break;

      case Datatype.INT:
        tempBuffer = new int[count*datatype.Size()] ;
        break;

      case Datatype.LONG:
        tempBuffer = new long[count*datatype.Size()] ;
        break;

      case Datatype.FLOAT:
        tempBuffer = new float[count*datatype.Size()] ;
        break;

      case Datatype.DOUBLE:
        tempBuffer = new double[count*datatype.Size()] ;
        break;

      case Datatype.PACKED: 
        System.out.println("Intracomm.createTemporaryBuffer() - "+
                           "PACKED datatype"); 
        break;

      case Datatype.OBJECT: 
        //System.out.println("Intracomm.createTemporaryBuffer() - "+
        //                   "OBJECT datatype"); 
	tempBuffer = new Object[count*datatype.Size()] ; 
        break;

      default:
        System.out.println("Intracomm.createTemporaryBuffer() - "+
                           "default datatype"); 
        break;
    }

    return tempBuffer ; 

  }

  public void Allreduce(Object sendbuf, int sendoffset,
                        Object recvbuf, int recvoffset, int count,
                        Datatype datatype, Op op) throws MPIException {

    if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
      MPI.logger.debug("Allreduce called");
    }
    
    if(op.worker == null) {

      if(MPI.DEBUG && MPI.logger.isDebugEnabled()) {
        MPI.logger.debug("User-defined ops ..but should not "+
			"see this for MAXLOC ");
      }
      Request req[] = new Request[Size()];

      //BUG: this is a bug, identified by Sarah Nakao
      //Object tmpbuf = sendbuf;
      Object tmpbuf = createTemporaryBuffer(datatype, count) ; 

      for (int i = 0; i < req.length; i++) {
        if(i != Rank()) 
          req[i] = isend(sendbuf, sendoffset, count, 
			 datatype, i, allreduceTag, false);
        else 
          System.arraycopy(sendbuf, sendoffset, tmpbuf, sendoffset, 
			  count*datatype.size);
      }
        
      for (int i = 0; i < Size(); i++) {
        if (i != Rank()) {
          recv(recvbuf, recvoffset, count, datatype, i, allreduceTag, false);
          op.funct.Call(recvbuf, recvoffset, tmpbuf, 
			  sendoffset, count, datatype);
	}
      }

      for (int i = 0; i < Size(); i++) {
        if (i != Rank())
          req[i].Wait();
      }

      //why is it count.datatype.size ???
      System.arraycopy(tmpbuf, sendoffset, recvbuf, recvoffset, 
		      count*datatype.size);
      return;

    } //end if user_defined datatypes.


    else {

      if(EXOTIC_ALLREDUCE) { 

        Op op2 = op.worker.getWorker(datatype);

        op2.createInitialBuffer(sendbuf, sendoffset, count*datatype.size );
        op2.getResultant(recvbuf, recvoffset, count);

        int mask = 0x1 ; 
        int i=0, dst ; 
        int me = Rank() ; 
        int size = Size() ; 

        while(mask < size) {

          dst = me ^ mask ;  

          sendrecv(recvbuf, sendoffset, count, datatype, dst, 
                   allreduceTag, sendbuf, recvoffset, count, datatype, 
                   dst, allreduceTag) ; 

          op2.perform(sendbuf, recvoffset, count);
          op2.getResultant(recvbuf, recvoffset, count);
          mask <<= 1; 
        } 

        return ; 
      }  

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("Pre defined ops");

      Op op2 = op.worker.getWorker(datatype);

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("got worker");

      op2.createInitialBuffer(sendbuf, sendoffset, count*datatype.size );

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("created initial buffer ");

      Request req[] = new Request[Size()];

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("sending messages ");

      for (int i = 0; i < req.length; i++) {
        if (i != Rank())
          req[i] = isend(sendbuf, sendoffset, count, datatype, i, 
	  		allreduceTag, false);
      }

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug("receiving messages ");

      for (int i = 0; i < Size(); i++) {
        if (i != Rank()) {
          recv(recvbuf, recvoffset, count, datatype, i, 
	  		allreduceTag, false);
          op2.perform(recvbuf, recvoffset, count);
        }
      }

      op2.getResultant(recvbuf, recvoffset, count);
    
      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
         MPI.logger.debug("got resultant ..");

      for (int i = 0; i < Size(); i++) {
        if (i != Rank())
          req[i].Wait();
      }

      if(MPI.DEBUG && MPI.logger.isDebugEnabled())
        MPI.logger.debug(" All reduce ends ");

      return ; 
    } 

  } //end Allreduce


  /**
   * Combine elements in input buffer of each process using the reduce
   * operation, and scatter the combined values over the output buffers
   * of the processes.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> recvcounts </tt></td><td> numbers of result elements
   *                                        distributed to each process </tr>
   * <tr><td><tt> datatype   </tt></td><td> data type of each item in send
   *                                        buffer </tr>
   * <tr><td><tt> op         </tt></td><td> reduce operation </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_REDUCE_SCATTER</tt>.
   */
  public void Reduce_scatter(Object sendbuf, int sendoffset,
                             Object recvbuf, int recvoffset, int[] recvcounts,
                             Datatype datatype, Op op) throws MPIException {

    int root = 0, myself = Rank();
    int sendcount = 0;

    for (int k = 0; k < recvcounts.length; k++) {
      sendcount += recvcounts[k];
    }

    Reduce(sendbuf, sendoffset, recvbuf, recvoffset, sendcount,
           datatype, op, root);
    Scatter(recvbuf, recvoffset, recvcounts[myself], datatype,
            recvbuf, recvoffset, recvcounts[myself],
            datatype, root);
  }

  /**
   * Perform a prefix reduction on data distributed across the group.
   * <p>
   * <table>
   * <tr><td><tt> sendbuf    </tt></td><td> send buffer array </tr>
   * <tr><td><tt> sendoffset </tt></td><td> initial offset in send buffer </tr>
   * <tr><td><tt> recvbuf    </tt></td><td> receive buffer array </tr>
   * <tr><td><tt> recvoffset </tt></td><td> initial offset in receive
   *                                        buffer </tr>
   * <tr><td><tt> count      </tt></td><td> number of items in input
   *                                        buffer </tr>
   * <tr><td><tt> datatype   </tt></td><td> data type of each item in input
   *                                        buffer </tr>
   * <tr><td><tt> op         </tt></td><td> reduce operation </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_SCAN</tt>.
   */

  public void Scan(Object sendbuf, int sendoffset,
                   Object recvbuf, int recvoffset, int count,
                   Datatype datatype, Op op) throws MPIException {

    if(op.worker == null) {

      Request req[] = new Request[Size()];
      //BUG:
      //Object tmpbuf = sendbuf;
      Object tmpbuf = createTemporaryBuffer(datatype, count) ; 
      System.arraycopy(sendbuf, sendoffset, tmpbuf, sendoffset, 
		      count*datatype.size);

      for (int i = (Size() - 1); i > Rank(); i--) {
        req[i] = isend(sendbuf, sendoffset, count, 
	               datatype, i, scanTag , false);
      }
        
      for (int i = 0; i < Rank(); i++) {
        recv(recvbuf, recvoffset, count, datatype, i, scanTag , false);
        op.funct.Call(recvbuf, recvoffset, tmpbuf, 
                      sendoffset, count, datatype);
      }

      for (int i = (Size() - 1); i > Rank(); i--) {
         req[i].Wait();
      }

      System.arraycopy(tmpbuf, sendoffset, recvbuf, recvoffset, 
		      count*datatype.size);
      return;

    }//end if user_defined datatypes.
    
    Op op2 = op.worker.getWorker(datatype);
    op2.createInitialBuffer(sendbuf , sendoffset, count); 
    Request req[] = new Request[Size()];
    //op2.perform(sendbuf, sendoffset, count);
    for (int i = (Size() - 1); i > Rank(); i--) {
      req[i] = isend(sendbuf, sendoffset, count, datatype, i, scanTag, false);
    }

    for (int i = 0; i < Rank(); i++) {
      recv(recvbuf, recvoffset, count, datatype, i, scanTag, false);
      op2.perform(recvbuf, recvoffset, count);
    }

    op2.getResultant(recvbuf, recvoffset, count);

    for (int i = (Size() - 1); i > Rank(); i--) {
      req[i].Wait();
    }

  }

  //Topology constructors
  /**
   * Create a Cartesian topology communicator whose group is a subset
   * of the group of this communicator.
   * <p>
   * <table>
   * <tr><td><tt> dims     </tt></td><td> the number of processes in each
   *                                      dimension </tr>
   * <tr><td><tt> periods  </tt></td><td> <tt>true</tt> if grid is periodic,
   *                                      <tt>false</tt> if not, in each
   *                                      dimension </tr>
   * <tr><td><tt> reorder  </tt></td><td> <tt>true</tt> if ranking may be
   *                                      reordered, <tt>false</tt> if not </tr>
   * <tr><td><em> returns: </em></td><td> new Cartesian topology
   *                                      communicator </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_CART_CREATE</tt>.
   * <p>
   * The number of dimensions of the Cartesian grid is taken to be the size
   * of the <tt>dims</tt> argument.  The array <tt>periods</tt> must be the
   * same size.
   */
  public Cartcomm Create_cart(int[] dims, boolean[] periods,
                              boolean reorder) throws MPIException {
    int totalProcess = this.group.Size();
    int places = 1;
    mpjdev.Comm ncomm = mpjdevComm;
    mpi.Group ngroup = this.group;

    for (int i = 0; i < dims.length; i++) {
      if (dims[i] < 0) {
        throw new MPIException("dims["+i+"] is less than than zero"); 
      }
      places *= dims[i];
    }

    //MPI.logger.debug("places "+places);
    //MPI.logger.debug("totalProcesses "+totalProcess);

    if (places > totalProcess) {
      throw new MPIException(" Error in Intracomm.Create_cart: "+
		      "total grid positions <"+places+"> are greater than "+
		      " total processes <"+totalProcess+">");
    }
    else if (places < totalProcess) {
      //MPI.logger.debug("dropping some processes ...");
      int[] excl = new int[totalProcess - places];
      //MPI.logger.debug("length of excl array "+excl.length);

      for (int i = 0; i < excl.length; i++) {
        excl[i] = i + places;
      }
      try {
        //MPI.logger.debug("calling excl method .");
        ngroup = ngroup.Excl(excl);
        //MPI.logger.debug( "called excl .. creating "+
        //		     "new group now" );
        ncomm = ncomm.create(ngroup.mpjdevGroup);
        //MPI.logger.debug("created the new comm");
      }
      catch (Exception e) {
        throw new MPIException(e);
      }

    }
    else {
      //MPI.logger.debug("All OK");
      ngroup = group;

      try {
        ncomm = ncomm.create(ngroup.mpjdevGroup);
      }
      catch (Exception e) {
        throw new MPIException(e );	      
      }

    }
    
    //MPI.logger.debug("oldRank "+group.Rank()+">,nrank<"+ngroup.Rank());
    if (ngroup.Rank() == -1) {
      //MPI.logger.debug("means this process is not in the "+
      //	    "new group");
      return null; //comm stuff ...
    }

    return new Cartcomm(dims, periods, reorder, ncomm,
                        ngroup.mpjdevGroup);
  }

  /**
   * Create a graph topology communicator whose group is a subset
   * of the group of this communicator.
   * <p>
   * <table>
   * <tr><td><tt> index    </tt></td><td> node degrees </tr>
   * <tr><td><tt> edges    </tt></td><td> graph edges </tr>
   * <tr><td><tt> reorder  </tt></td><td> <tt>true</tt> if ranking may be
   *                                      reordered, <tt>false</tt> if not </tr>
   * <tr><td><em> returns: </em></td><td> new graph topology communicator </tr>
   * </table>
   * <p>
   * Java binding of the MPI operation <tt>MPI_GRAPH_CREATE</tt>.
   * <p>
   * The number of nodes in the graph, <em>nnodes</em>, is taken
   * to be size of the <tt>index</tt> argument.  The size of array
   * <tt>edges</tt> must be <tt>index [nnodes} - 1]</tt>.
   */
  public Graphcomm Create_graph(int[] index, int[] edges, boolean reorder) {
    int size = this.group.Size();
    int nnodes = index.length;
    mpjdev.Comm ncomm = mpjdevComm;
    mpi.Group ngroup = this.group;

    //MPI.logger.debug("nnodes "+nnodes);
    //MPI.logger.debug("size "+size);

    if (nnodes > size) {
      throw new MPIException(" Error in Intracomm.Create_graphs: "+
		      "total nodes <"+nnodes+"> are greater than "+
		      " total processes <"+ size +">");
    }
    else if (nnodes < size) {
      //MPI.logger.debug("dropping some processes ...");
      int[] excl = new int[size - nnodes];
      //MPI.logger.debug("length of excl array "+excl.length);

      for (int i = 0; i < excl.length; i++) {
        excl[i] = i + nnodes;
      }

      try {
        //MPI.logger.debug("calling excl method .");
        ngroup = ngroup.Excl(excl);
        //MPI.logger.debug( "called excl .. creating "+
        //	    "new group now" );
        ncomm = ncomm.create(ngroup.mpjdevGroup);
        //MPI.logger.debug("created the new comm");
      }
      catch (Exception e) {
        throw new MPIException(e );	      
      }

    }
    else {
      //MPI.logger.debug("All OK");
      ngroup = group;

      try {
        ncomm = ncomm.create(ngroup.mpjdevGroup);
      }
      catch (Exception e) {
        throw new MPIException(e);	      
      }

    }

//MPI.logger.debug("oldRank "+group.Rank()+">,nrank<"+ngroup.Rank());

    if (ngroup.Rank() == -1) {
      //MPI.logger.debug("means this process is not in the "+
      //	 		    "new group");
      return null; //comm stuff ...
    }

    return new Graphcomm(index, edges, reorder, ncomm,
                         ngroup.mpjdevGroup);
  }
}

