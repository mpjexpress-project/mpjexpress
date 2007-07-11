/*
 The MIT License

 Copyright (c) 2005 - 2007
   1. Distributed Systems Group, University of Portsmouth (2005)
   2. Aamir Shafi (2005 - 2007)
   3. Bryan Carpenter (2005 - 2007)
   4. Mark Baker (2005 - 2007)

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
 * File         : MPJDaemon.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.28 $
 * Updated      : $Date: 2006/10/20 17:24:47 $
 */

package runtime.daemon;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;

import org.apache.log4j.Logger ;
import org.apache.log4j.PropertyConfigurator ;
import org.apache.log4j.PatternLayout ;
import org.apache.log4j.FileAppender ;
import org.apache.log4j.Level ;
import org.apache.log4j.DailyRollingFileAppender ;
import org.apache.log4j.spi.LoggerRepository ;

import runtime.MPJRuntimeException ;  

import java.util.concurrent.Semaphore ; 

public class MPJDaemon {

  private SocketChannel peerChannel; 
  private BufferedReader reader = null;
  private InputStream outp = null;
  private int D_SER_PORT = 10000;
  private boolean loop = true;
  private Selector selector = null;
  private volatile boolean selectorAcceptConnect = true;
  private volatile boolean kill_signal = false;
  private Thread[] workers = null;
  private volatile boolean wait = true;
  private volatile boolean waitToStartExecution = true;
  private String hostName = null; 
  private PrintStream out = null;
  private Semaphore outputHandlerSem = new Semaphore(1,true); 
  private static final boolean DEBUG = false ; 
  
  private String wdir = null ; 
  private int numOfProcs = 0; 
  private int pos = 0; 
  private String URL = null; //http:server:portclient.jar
  private String mpjURL = null; 
  private String deviceName = null;
  private String className = null ;
  private String mpjHome = null ;
  private ArrayList<String> jvmArgs = new ArrayList<String>();
  private ArrayList<String> appArgs = new ArrayList<String>();
  private int processes = 0;
  private String cmd = null;
  private Process[] processVector = null;
  private static Logger logger = null ; 
  private String mpjHomeDir = null ;  
  private String loader = null; 

  /** 
   * The constructor which does everything ...
   */
  public MPJDaemon(String args[]) throws Exception {
	  
    numOfProcs = Runtime.getRuntime().availableProcessors();
    InetAddress localaddr = InetAddress.getLocalHost();
    hostName = localaddr.getHostName();
    
    Map<String,String> map = System.getenv() ;
    mpjHomeDir = map.get("MPJ_HOME");
			    
    createLogger(mpjHomeDir); 

    if(DEBUG && logger.isDebugEnabled()) { 
      logger.debug("mpjHomeDir "+mpjHomeDir); 
    }

    if (args.length == 1) {
	    
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug (" args[0] " + args[0]);
        logger.debug ("setting daemon port to" + args[0]);
      }

      D_SER_PORT = new Integer(args[0]).intValue();

    }
    else {
	    
      throw new MPJRuntimeException("Usage: java MPJDaemon daemonServerPort");

    }

    serverSocketInit();
    Thread selectorThreadStarter = new Thread(selectorThread);
    
    if(DEBUG && logger.isDebugEnabled()) { 
      logger.debug ("Starting the selector thread ");
    }

    selectorThreadStarter.start();
    int exit = 0;

    while (loop) {

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("MPJDaemon is waiting to accept connections ... ");
      }
      
      wdir = System.getProperty("user.dir");
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("wdir "+wdir);
      }
      waitToStartExecution ();

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("A client has connected");
      }

      MPJProcessPrintStream.start(peerChannel, 
                       new PrintStream(System.out),
                       new PrintStream(System.err));

      /* 
       *  The client request more than one processes ..and we dont wanne 
       *  use  smpdev ...it means start two separate JVMs ... 
       */
      if (!deviceName.equals("smpdev") && processes > 1) { 
  
        workers = new Thread[processes];
        synchronized (this) {
          processVector = new Process[processes];
        }
        BufferedReader bufferedReader = null;
        InputStream in = null;
        URL aURL = null;
        String conf = URL.substring(0, (URL.lastIndexOf("/") + 1));
        String config = conf + "mpjdev.conf";
        String rank_ = null;

        try {
          aURL = new URL(new String(config));
          in = aURL.openStream();
        }
        catch (Exception e) {
          e.printStackTrace();
        }

        bufferedReader = new BufferedReader(new InputStreamReader(in));

        int iter = 0;

        for (int j = 0; j < processes; j++) {
          String line = null;
          boolean loop = true;

          while (loop) {
            line = bufferedReader.readLine();
	    
            if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug ("line read <" + line + ">");
	    }
	    
            if ( (line != null) &&
                (matchMe(line))) {  

              StringTokenizer tokenizer = new StringTokenizer(line, "@");
              tokenizer.nextToken();
              tokenizer.nextToken();
              rank_ = tokenizer.nextToken();

              if(DEBUG && logger.isDebugEnabled()) { 
                logger.debug ("rank_ " + rank_);
	      }
	      
              loop = false;

            }
            else {
              iter++;
              if (iter > (processes + 100)) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug (" read all entries from config file");
		}
                loop = false;
              }
              else {
                continue;
	      }
            } //end else


          } //end while

	  String[] jArgs = jvmArgs.toArray(new String[0]); 
          boolean now = false;
          boolean noSwitch = true ;

          for(int e=0 ; e<jArgs.length; e++) {

            if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug("jArgs["+e+"]="+jArgs[e]);
	    }

            if(now) {
		    
              String cp = jvmArgs.remove(e);
	      
	      if(loader.equals("useLocalLoader")) {
                cp = "."+File.pathSeparator+""+
                       mpjHomeDir+"/lib/loader1.jar"+
                       File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                       File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                       File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
                       File.pathSeparator+cp;
	      }
	      
	      else if(loader.equals("useRemoteLoader")) {
                cp = "."+File.pathSeparator+""+
                  mpjHomeDir+"/lib/loader1.jar"+
	          File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                  File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
                  File.pathSeparator+cp;
	      }
	      
              jvmArgs.add(e,cp);
              now = false;
            }

            if(jArgs[e].equals("-cp")) {
              now = true;
              noSwitch = false;
            }

          }

          if(noSwitch) {
            jvmArgs.add("-cp");

	    if(loader.equals("useLocalLoader")) {
	      jvmArgs.add("."+File.pathSeparator+""
  	        +mpjHomeDir+"/lib/loader1.jar"+
                File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar" );
	    }

	    else if(loader.equals("useRemoteLoader")) {
              jvmArgs.add("."+File.pathSeparator+""+
 	        mpjHomeDir+"/lib/loader1.jar"+
		File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
		File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar" );
	    }
	  }

          jArgs = jvmArgs.toArray(new String[0]);

          for(int e=0 ; e<jArgs.length; e++) {
		  
            if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug("modified: jArgs["+e+"]="+jArgs[e]);
	    }

          }
	  
	  String[] aArgs = appArgs.toArray(new String[0]); 
	  
          String[] ex = new String[ (9+jArgs.length+aArgs.length) ]; 
	  ex[0] = "java";

	  //System.arraycopy ... can be used ..here ...
	  for(int i=0 ; i<jArgs.length ; i++) { 
	    ex[i+1] = jArgs[i]; 	
	  }

	  int indx = jArgs.length+1; 
	
	  ex[indx] = "runtime.daemon.Wrapper" ; indx++ ;
	  ex[indx] = URL; indx++ ; 
	  ex[indx] = Integer.toString(processes); indx++ ; 
	  ex[indx] = deviceName; indx++ ; 
	  ex[indx] = rank_ ; indx++ ;
	  ex[indx] = loader ; indx++ ;
	  ex[indx] = mpjURL; indx++ ;
	  
	  if(className != null) {
	    ex[indx] = className ; 
	  }
	  else {
	    ex[indx] = "dummy" ; //this is JAR case ..this arg will never 
	                               //be used ...
	  }

	  //System.arraycopy ... can be used ..here ...
	  for(int i=0 ; i< aArgs.length ; i++) { 
	    ex[i+9+jArgs.length] = aArgs[i]; 	
	  }

	  if(DEBUG && logger.isDebugEnabled()) { 
            for (int i = 0; i < ex.length; i++) {
              logger.debug(i+": "+ ex[i]);
            }  
	  }
	
	  /*... Making the command finishes here ...*/
          ProcessBuilder pb = new ProcessBuilder(ex);
	  pb.directory(new File(wdir)) ;
          pb.redirectErrorStream(true); 
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("starting the process ");
	  }
          Process p = pb.start();
	  
          synchronized (processVector) {
            processVector[j] = p;
          }
	  
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("started the process "); 
	  }

        } //end for loop.

        for (int j = 0; j < processes; j++) {
          outputHandlerSem.acquireUninterruptibly() ; 
          pos = j;
          workers[j] = new Thread(outputHandler);
          workers[j].start();
        }

        for (int j = 0; j < processes; j++) {
          workers[j].join();
        }

      }
      else {
	      
        workers = new Thread[1];
	
        synchronized (this) {
          processVector = new Process[1];
        }

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("the daemon will start <" + processes + "> threads"); 
	}

        String[] jArgs = jvmArgs.toArray(new String[0]);
	
	boolean now = false;
	boolean noSwitch = true ; 

	for(int e=0 ; e<jArgs.length; e++) {
		
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("jArgs["+e+"]="+jArgs[e]);		
	  }
	  
	  if(now) {
            String cp = jvmArgs.remove(e); 

	    if(loader.equals("useLocalLoader")) { 
	      cp = "."+File.pathSeparator+""+
	  	      mpjHomeDir+"/lib/loader2.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
	  	      File.pathSeparator+cp;
	    }
	    else if(loader.equals("useRemoteLoader")) { 
	      cp = "."+File.pathSeparator+""+
	  	mpjHomeDir+"/lib/loader2.jar"+
                File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"+
	  	File.pathSeparator+cp;
	    }
	    
	    jvmArgs.add(e,cp);
            now = false;		  
	  }
	  
	  if(jArgs[e].equals("-cp")) {
            now = true;
	    noSwitch = false;
	  }
	  
	}

	if(noSwitch) {
	  jvmArgs.add("-cp");

	  if(loader.equals("useLocalLoader")) {
	    jvmArgs.add("."+File.pathSeparator+""+
	              mpjHomeDir+"/lib/loader2.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"
		  	  );
	  }

	  else if(loader.equals("useRemoteLoader")) {
	    jvmArgs.add("."+File.pathSeparator+""+
	      mpjHomeDir+"/lib/loader2.jar"+
              File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
              File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"
	    );
	  }
	}

	jArgs = jvmArgs.toArray(new String[0]);

	for(int e=0 ; e<jArgs.length; e++) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("modified: jArgs["+e+"]="+jArgs[e]);		
	  }
	}
	
	String[] aArgs = appArgs.toArray(new String[0]);
        String[] ex =
		new String[ (8+jArgs.length+aArgs.length) ];
	ex[0] = "java";
	
	for(int i=0 ; i< jArgs.length ; i++) {
	  ex[i+1] = jArgs[i];
	}

	int indx = jArgs.length+1;
	
	ex[indx] = "runtime.daemon.ThreadedWrapper" ; indx++ ; 
	ex[indx] = URL; indx++ ; 
	ex[indx] = Integer.toString(processes); indx++ ; 
	ex[indx] = deviceName; indx++;
	ex[indx] = loader; indx++;
	ex[indx] = mpjURL ; indx++;
	
	if(className != null) {
	  ex[indx] = className;   
	}
	else {
	  ex[indx] = "dummy" ; //this is JAR case ..this arg will never 
	                               //be used ...
	}
	
	for(int i=0 ; i< aArgs.length ; i++) {
	  ex[i+8+jArgs.length] = aArgs[i];
	}
		  
        for (int i = 0; i < ex.length; i++) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug(i+": "+ ex[i]);
	  }
        } 
	
	/*... Making the command finishes here ...*/

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("creating process-builder object ");
	}
        ProcessBuilder pb = new ProcessBuilder(ex);
	pb.directory(new File(wdir)); 
	//Map<String, String> m = pb.environment(); 
	//for(String str : m.values()) {
        //  if(DEBUG && logger.isDebugEnabled()) { 
        //    logger.debug("str : "+str);		
	//  }
	//}
        pb.redirectErrorStream(true);
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("starting the ThreadedWrapper.");
	}
        Process p = null;

        try {
          p = pb.start();
        }
        catch (Exception e) {
          e.printStackTrace();
        }

        synchronized (processVector) {
          processVector[0] = p;
        }

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("started the ThreadedWrapper.");
	}
        outputHandlerSem.acquireUninterruptibly() ; 
        pos = 0;
        workers[0] = new Thread(outputHandler);
        workers[0].start();
        
        //System.out.println("calling join" + hostName);
        workers[0].join();
        //System.out.println("called join" + hostName);

      } //end else (ThreadedWrapper case)

      MPJProcessPrintStream.stop();

      synchronized (processVector) {
        for (int i = 0; i < processVector.length; i++) {
          processVector[i].destroy();
        }
        kill_signal = false;
      }

      workers = null;
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Stopping the output");
      }

      try {
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("Checking whether peerChannel is closed or what ?" +
                    peerChannel.isOpen());
	}

        if (peerChannel.isOpen()) {
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug ("Closing it ..."+peerChannel );
	  }
          peerChannel.close();
        }

        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("Was already closed, or i closed it");
	}
      }
      catch (Exception e) { 
        e.printStackTrace() ; 
        continue;
      }

      restoreVariables() ; 

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("\n\n ** .. execution ends .. ** \n\n");
      }

    } //end while(true)
  }

  private void restoreVariables() {
    pos = 0; 
    jvmArgs.clear();
    appArgs.clear(); 
    wdir = null ; 
    URL = null;
    mpjURL = null; 
    deviceName = null;
    className = null ;
    processes = 0;
    processVector = null;
    loader = null; 
  }

   private synchronized void waitToStartExecution () {

    while (waitToStartExecution) {
      try {
        this.wait();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    } 

    waitToStartExecution = true ; 

  }

  static boolean matchMe(String line) throws Exception {

    if(!line.contains("@") || line.startsWith("#") ) {
      return false;
    }

    StringTokenizer token = new StringTokenizer(line, "@");	  
    boolean found = false; 
    String hostName = token.nextToken() ;
    InetAddress host = null ;
    
    try {    
      host = InetAddress.getByName(hostName) ;
    }
    catch(Exception e){
      return false;   	    
    }

    Enumeration<NetworkInterface> cards =
            NetworkInterface.getNetworkInterfaces() ;
    
    foundIt: 

    while(cards.hasMoreElements()) {

      NetworkInterface card = cards.nextElement() ;
      Enumeration<InetAddress> addresses = card.getInetAddresses();

      while(addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement() ;
        if(host.getHostName().equals(address.getHostName()) || 
           host.getHostAddress().equals(address.getHostAddress())) {
          found = true;
          break foundIt;
        }

      }

    }

    return found; 
  }

  private synchronized void startExecution () {
    waitToStartExecution = false;
    this.notify();
  }
  
  private void createLogger(String homeDir) throws MPJRuntimeException {
  
    if(logger == null) {

      DailyRollingFileAppender fileAppender = null ;

      try {
        fileAppender = new DailyRollingFileAppender(
                            new PatternLayout(
                            " %-5p %c %x - %m\n" ),
                            homeDir+"/logs/daemon-"+hostName+".log",
                            "yyyy-MM-dd-a" );

        Logger rootLogger = Logger.getRootLogger() ;
        rootLogger.addAppender( fileAppender);
        LoggerRepository rep =  rootLogger.getLoggerRepository() ;
        rootLogger.setLevel ((Level) Level.ALL );
        logger = Logger.getLogger( "mpjdaemon" );
      }
      catch(Exception e) {
        throw new MPJRuntimeException(e) ;
      }
    }
  }



  private void serverSocketInit() {
    ServerSocketChannel serverChannel;
    try {
      selector = Selector.open();
      serverChannel = ServerSocketChannel.open();
      serverChannel.configureBlocking(false);
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Binding the serverSocketChannel @" + D_SER_PORT);
      }
      serverChannel.socket().bind(new InetSocketAddress(D_SER_PORT));
      serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    catch (Exception cce) {
      cce.printStackTrace();
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Exception in serverSocketInit()" + cce.getMessage());
      }
      System.exit(0);
    }
  }

  private void doAccept(SelectableChannel keyChannel) {
    if(DEBUG && logger.isDebugEnabled()) { 
      logger.debug ("---doAccept---");
    }

    try {
      peerChannel = ( (ServerSocketChannel) keyChannel).accept();
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("peerChannel " + peerChannel);
      }
    }
    catch (IOException ioe) {
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("IOException in doAccept");
      }
      System.exit(0);
    }

    try {
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("configuring the channel to be non-blocking");
      }
      peerChannel.configureBlocking(false);
    }
    catch (IOException ioe) {
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("IOException in doAccept");
      }
      System.exit(0);
    }

    try {
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Registering for OP_READ & OP_WRITE event");
      }
      peerChannel.register(selector,
                           SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    catch (ClosedChannelException cce) {
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("Closed Channel Exception in doAccept");
      }
      System.exit(0);
    }

    try {
      peerChannel.socket().setTcpNoDelay(true);
    }
    catch (Exception e) {}
  }

  Runnable selectorThread = new Runnable() {

    /* This is selector thread */
    public void run() {

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug ("selector Thread started ");
      }

      Set readyKeys = null;

      Iterator readyItor = null;

      SelectionKey key = null;

      SelectableChannel keyChannel = null;

      /* why are these required here? */
      SocketChannel socketChannel = null;
      ByteBuffer lilBuffer = ByteBuffer.allocateDirect(8);
      ByteBuffer lilBuffer2 = ByteBuffer.allocateDirect(4);
      ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
      byte[] lilArray = new byte[4];

      try {
        while (selector.select() > -1) {

          readyKeys = selector.selectedKeys();
          readyItor = readyKeys.iterator();

          while (readyItor.hasNext()) {

            key = (SelectionKey) readyItor.next();
            readyItor.remove();
            keyChannel = (SelectableChannel) key.channel();
            if(DEBUG && logger.isDebugEnabled()) { 
              logger.debug ("\n---selector EVENT---");
	    }

            if (key.isAcceptable() && selectorAcceptConnect) {
              doAccept(keyChannel);
            }
            else if (key.isConnectable()) {

              /*
               * why would this method be called?
               * At the daemon, this event is not generated ..
               */

              try {
                socketChannel = (SocketChannel) keyChannel;
              }
              catch (NoConnectionPendingException e) {
                continue;
              }

              if (socketChannel.isConnectionPending()) {
                try {
                  socketChannel.finishConnect();
                }
                catch (IOException e) {
                  continue;
                }
              }
              //doConnect(socketChannel);
            }

            else if (key.isReadable()) {

              if(DEBUG && logger.isDebugEnabled()) { 
                logger.debug ("READ_EVENT");
	      }
	      
              socketChannel = (SocketChannel) keyChannel;
	      
              int readInt = -1 ; 
	      
              if(DEBUG && logger.isDebugEnabled()) { 
                logger.debug("lilBuffer "+ lilBuffer);         
	      }

	        // .. this line is generating exception which kills the 
		//    daemon ... I think we need a try catch here and if
		//    any exception is generated, then we will have to 
		//    goto back to selector.select() method ..
		//
		//    this is Windows 2000 specific error ..i have not 
		//    seen this error on Windows XP ..
	      
	      try { 
		      
                if((readInt = socketChannel.read(lilBuffer)) == -1) {

                  if(DEBUG && logger.isDebugEnabled()) { 
                    logger.debug(" The daemon has received an End of "+
	  	  		  "Stream signal") ; 
	    	    logger.debug(" checking if this channel is still open");
		  }
		  
		  if(socketChannel.isOpen()) {
                    if(DEBUG && logger.isDebugEnabled()) { 
                      logger.debug("closing the channel");
		    }
		    socketChannel.close() ; 			  
		  }

                  if(DEBUG && logger.isDebugEnabled()) { 
                    logger.debug("continuing to select()");
		  }
		  continue ; 
                  //END_OF_STREAM signal .... 

                }
	      }
	      catch(Exception innerException) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Exception in selector thread, message is"+
				  innerException.getMessage() );
                  logger.debug (" continuing to select() method ..."); 
		}
		continue; 
	      }
		      

                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("READ_EVENT (read)" + readInt);
		}

              lilBuffer.flip();
              lilBuffer.get(lilArray, 0, 4);
              String read = new String(lilArray);
              if(DEBUG && logger.isDebugEnabled()) { 
                logger.debug ("READ_EVENT (String)<" + read + ">");
	      }

              if (read.equals("url-")) {
 	        if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("url-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("URL Length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                URL = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("URL:<" + URL + ">");
		}
		
		if(URL.endsWith(".jar")) {
                  className = null ;  			
		}
		
                buffer.clear();
              }
	      
              if (read.equals("mul-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("mul-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("URL Length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                mpjURL = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("mpjURL:<" + mpjURL + ">");
		}
                buffer.clear();
              }
	      
              if (read.equals("cls-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("cls-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("className length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                className = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("className :<" + className + ">");
		}
                buffer.clear();
              }

              if (read.equals("app-")) {
		      
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("app-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Application args Length -->" + length);
		}
                lilBuffer.clear();

		for(int j=0 ; j<length ; j++) {
                  lilBuffer2.position(0); lilBuffer2.limit(4); 			
                  socketChannel.read(lilBuffer2);
		  lilBuffer2.flip();
                  int argLen = lilBuffer2.getInt();
                  buffer.limit(argLen);
                  socketChannel.read(buffer);
                  byte[] t = new byte[argLen];
                  buffer.flip();
                  buffer.get(t,0,argLen);
		  appArgs.add(new String(t)); 
		  buffer.clear(); 
		  lilBuffer2.clear();
		}
                 
		//for loop to create a new array ...
                buffer.clear();
		
              }
              else if (read.equals("num-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("num-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("should be 4, isit ? -->" + length);
		}
                lilBuffer.clear();
                socketChannel.read(lilBuffer2);
                lilBuffer2.flip();
                processes = lilBuffer2.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Num of processes ==>" + processes);
		}
                lilBuffer2.clear();
              }

              else if (read.equals("arg-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("arg-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("argu len -->"+length);
		}
                lilBuffer.clear();

		for(int j=0 ; j<length ; j++) {
                  lilBuffer2.position(0); lilBuffer2.limit(4); 			
                  socketChannel.read(lilBuffer2);
		  lilBuffer2.flip();
                  int argLen = lilBuffer2.getInt();
                  buffer.limit(argLen);
                  socketChannel.read(buffer);
                  byte[] t = new byte[argLen];
                  buffer.flip();
                  buffer.get(t,0,argLen);
		  jvmArgs.add(new String(t)); 
		  buffer.clear(); 
		  lilBuffer2.clear();
		}
                 
		//for loop to create a new array ...
                buffer.clear();
		
              }

              else if (read.equals("dev-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("dev-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("dev-Length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                deviceName = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Device Name :<" + deviceName + ">");
		}
                buffer.clear();
              }
	      
              else if (read.equals("ldr-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("ldr-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("ldr-Length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                loader = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("ldr:<"+loader+">");
		}
                buffer.clear();
              }

              else if (read.equals("wdr-")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("wdr-");
		}
                int length = lilBuffer.getInt();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("wdr-Length -->" + length);
		}
                lilBuffer.clear();
                buffer.limit(length);
                socketChannel.read(buffer);
                byte[] byteArray = new byte[length];
                buffer.flip();
                buffer.get(byteArray, 0, length);
                wdir = new String(byteArray);
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("wdir :<"+wdir+">");
		}
                buffer.clear();
              }

              else if (read.equals("*GO*")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("GO");
		}
                lilBuffer.clear();
                startExecution ();

              }
              else if (read.equals("kill")) {
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("processing kill event");
		}
                MPJProcessPrintStream.stop();
                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Stopping the output");
		}

                try {
                  if(DEBUG && logger.isDebugEnabled()) { 
                    logger.debug ("peerChannel is closed or what ?" +
                                peerChannel.isOpen());
		  }

                  if (peerChannel.isOpen()) {
                    if(DEBUG && logger.isDebugEnabled()) { 
                      logger.debug ("Closing it ...");
		    }
                    peerChannel.close();
                  }
                }
                catch (Exception e) {}

                  if(DEBUG && logger.isDebugEnabled()) { 
                    logger.debug ("Killling the process");
		  }
                try {
                  synchronized (MPJDaemon.this) {
                    if (processVector != null) {
                      synchronized (processVector) {

                        for (int i = 0; i < processVector.length; i++) {
                          processVector[i].destroy();
                        }

                        kill_signal = true;
                      }
                    }
                  }
                }
                catch (Exception e) {} 
//no matter what happens, we cant let this thread
//die, coz otherwise, the daemon will die as well..
//maybe you wanne stop the output handler threads as well.

                if(DEBUG && logger.isDebugEnabled()) { 
                  logger.debug ("Killed it");
		}
                buffer.clear();
                lilBuffer.clear();

              }

            } //end if key.isReadable()

            else if (key.isWritable()) {

              if(DEBUG && logger.isDebugEnabled()) { 
                logger.debug(
                    "In, WRITABLE, so changing the interestOps to READ_ONLY");
	      }
              key.interestOps(SelectionKey.OP_READ);

            }

          } //end while iterator
        } //end while
      }
      catch (Exception ioe1) {
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("Exception in selector thread " + ioe1.getMessage());
	}
        ioe1.printStackTrace();
        //System.exit(0);
      }
    } //end run()
  }; //end selectorThread which is an inner class 

  Runnable outputHandler = new Runnable() {

    public void run() {

      Process p = null;

      synchronized (processVector) {
        p = processVector[pos];
      }

      outputHandlerSem.release() ; 
      String line = "";
      InputStream outp = p.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(outp));
       
      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug( "outputting ...");
      }

      try {
        do {
          if (!line.equals("")) {
            line.trim(); 
 
            synchronized (this) {
              System.out.println(line);
              //if(DEBUG && logger.isDebugEnabled()) { 
              //  logger.debug(line);
	      //}
            } 
          }
        }  while ( (line = reader.readLine()) != null); 
        // && !kill_signal); 
      }
      catch (Exception e) {
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug ("outputHandler =>" + e.getMessage());
	}
        e.printStackTrace();
      } 
    } //end run.
  }; //end outputHandler.

  public static void main(String args[]) {
    try {
      MPJDaemon dae = new MPJDaemon(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

            //"-Xloggc:" + hostName + ".gc",
            //"-XX:+PrintGCDetails",
            //"-XX:+PrintGCTimeStamps",
            //"-XX:+PrintGCApplicationConcurrentTime",
            //"-XX:+PrintGCApplicationStoppedTime",
            //"-Xnoclassgc",
            //"-XX:MinHeapFreeRatio=5",
            //"-XX:MaxHeapFreeRatio=5",
            //"-Xms512M", "-Xmx512M",
            //"-DSIZE=1000", "-DITERATIONS=100",
            //"-Xdebug",
            //"-Xrunjdwp:transport=dt_socket,address=11000,server=y,suspend=n",
}
