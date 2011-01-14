/*
 The MIT License

 Copyright (c) 2005 - 2011
   1. Distributed Systems Group, University of Portsmouth (2005)
   2. Aamir Shafi (2005 - 2011)
   3. Bryan Carpenter (2005 - 2011)
   4. Mark Baker (2005 - 2011)

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
 * File         : MPJRun.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.35 $
 * Updated      : $Date: Wed Jan  5 20:55:53 EST 2011$
 */

package runtime.starter;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.Logger ;
import org.apache.log4j.PropertyConfigurator ;
import org.apache.log4j.PatternLayout ;
import org.apache.log4j.FileAppender ;
import org.apache.log4j.Level ;
import org.apache.log4j.DailyRollingFileAppender ;
import org.apache.log4j.spi.LoggerRepository ;

import java.util.jar.Attributes ;
import java.util.jar.JarFile ;

import runtime.MPJRuntimeException ;

public class MPJRun {

  final int DEFAULT_MPJ_SERVER_PORT = 20000;
  final int DEFAULT_S_PORT = 15000; 
  final String DEFAULT_MACHINES_FILE_NAME = "machines"; 
  final int DEFAULT_PROTOCOL_SWITCH_LIMIT = 128*1024; //128K

  private String CONF_FILE_CONTENTS  = "#temp line" ;
  private int MPJ_SERVER_PORT = DEFAULT_MPJ_SERVER_PORT;
  private int mxBoardNum = 0 ; 
  private int D_SER_PORT = getPortFromWrapper() ;
  private int endPointID = 0 ;
  private int S_PORT = DEFAULT_S_PORT; 
  String machinesFile = DEFAULT_MACHINES_FILE_NAME;
  private int psl = DEFAULT_PROTOCOL_SWITCH_LIMIT;

  ArrayList<String> jvmArgs = new ArrayList<String>() ; 
  ArrayList<String> appArgs = new ArrayList<String>() ; 
  String[] jArgs = null ;  
  String[] aArgs = null ;
  static Logger logger = null ; 
  private volatile boolean wait = true;
  private Vector<SocketChannel> peerChannels;
  private Selector selector = null;
  private volatile boolean selectorFlag = true;
  private String hostIP = null;
  private Thread selectorThreadStarter = null;
  private Vector machineVector = new Vector();
  int nprocs = Runtime.getRuntime().availableProcessors() ; 
  String deviceName = "multicore";
  String mpjHomeDir = null;
  byte[] urlArray = null;
  Hashtable procsPerMachineTable = new Hashtable();
  int endCount = 0; 
  int streamEndedCount = 0 ;
  String wdir;
  String className = null ; 
  String applicationClassPathEntry = null ; 
  ByteBuffer buffer = null;

  static final boolean DEBUG = false ; 
  static final String VERSION = "0.38" ; 
  private static int RUNNING_JAR_FILE = 2 ; 
  private static int RUNNING_CLASS_FILE = 1 ; 

  /**
   * Every thing is being inside this constructor :-)
   */
  public MPJRun(String args[]) throws Exception {

    java.util.logging.Logger logger1 = 
    java.util.logging.Logger.getLogger("");


    //remove all existing log handlers: remove the ERR handler
    for (java.util.logging.Handler h : logger1.getHandlers()) {
      logger1.removeHandler(h);
    }
		  
    Map<String,String> map = System.getenv() ;
    mpjHomeDir = map.get("MPJ_HOME");

    createLogger(args) ; 

    if(DEBUG && logger.isDebugEnabled()) {
      logger.info(" --MPJRun invoked--"); 
      logger.info(" adding shutdown hook thread"); 
    }

	    
    if(DEBUG && logger.isDebugEnabled()) {
      logger.info("processInput called ..."); 
    }

    processInput(args);

    /* the code is running in the multicore configuration */
    if(deviceName.equals("multicore")) {
       
      System.out.println("MPJ Express ("+VERSION+") is started in the "+
                                              "multicore configuration"); 
      if(DEBUG && logger.isDebugEnabled()) {
        logger.info("className "+className) ; 
      }

      int jarOrClass = (applicationClassPathEntry.endsWith(".jar")?
                                  RUNNING_JAR_FILE:RUNNING_CLASS_FILE);
       
      MulticoreDaemon multicoreDaemon =
          new MulticoreDaemon(className, applicationClassPathEntry, jarOrClass, 
	                           nprocs, wdir, jvmArgs, appArgs) ;
      return ;

    } else { /* cluster configuration */
      System.out.println("MPJ Express ("+VERSION+") is started in the "+
                                              "cluster configuration"); 
    }

    readMachineFile();
    machinesSanityCheck() ;
    assignTasks();

    urlArray = applicationClassPathEntry.getBytes();

    peerChannels = new Vector<SocketChannel>();
    selector = Selector.open();
    clientSocketInit();
    selectorThreadStarter = new Thread(selectorThread);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("Starting the selector thread ");
    }

    selectorThreadStarter.start();

    //wait till this client has connected to all daemons
    Wait();

    int peersStartingRank = 0; 

    for (int j = 0; j < peerChannels.size(); j++) {

      SocketChannel socketChannel = peerChannels.get(j);
      
      if(DEBUG && logger.isDebugEnabled()) { 
	     logger.debug("procsPerMachineTable " + procsPerMachineTable);
      }

      /* FIXME: should we not be checking all IP addresses of remote 
                machine? Does it make sense? */

      String hAddress = 
                     socketChannel.socket().getInetAddress().getHostAddress();
      String hName = socketChannel.socket().getInetAddress().getHostName();

      Integer nProcessesInt = ((Integer) procsPerMachineTable.get(hName)) ; 

      if(nProcessesInt == null) { 
        nProcessesInt = ((Integer) procsPerMachineTable.get(hAddress)) ;     
      } 

      int nProcesses = nProcessesInt.intValue();
	  /* FIX ME By Amjad Aziz & Rizwan Hanif
	  *  sending the starting rank to peer processi.e Daemon*/
      pack(nProcesses, peersStartingRank); 
      peersStartingRank += nProcesses;

      if(DEBUG && logger.isDebugEnabled()) { 
	    logger.debug("Sending to " + socketChannel);
      }

      int w = 0 ; 
      while(buffer.hasRemaining()) {
	  if((w += socketChannel.write(buffer)) == -1) {
	  //throw an exception ...
	  } 
      }
      if(DEBUG && logger.isDebugEnabled()) { 
		logger.debug("Wrote bytes-->"+w+"to process"+j);
      }

      buffer.clear();

    }

    if(DEBUG && logger.isDebugEnabled()) { 
      logger.debug("procsPerMachineTable " + procsPerMachineTable);
    }

    addShutdownHook();

    /* waiting to get the answer from the daemons that the job has finished. */
    Wait();

    if(DEBUG && logger.isDebugEnabled())
      logger.debug("Calling the finish method now");

    this.finish();

  }
	  
  /* 
   * 1.  Application Classpath Entry (cpe-). This is a String classpath entry 
         which will be appended by the MPJ Express daemon before starting
         a user process (JVM). In the case of JAR file, it's the absolute
         path and name. In the case of a class file, its the name of the 
         working directory where mpjrun command was launched. 
   * 2.  num- [# of processes] to be started by a particular MPJ Express
         daemon.
   * 3.  srk- [starting #(rank) of process] to be started by a 
         particular MPJ Express daemon.	
   * 4.  arg- args to JVM
   * 5.  wdr- Working Directory 
   * 6.  cls- Classname to be executed. In the case of JAR file, this 
         name is taken from the manifest file. In the case of class file, 
         the class name is specified on the command line by the user.
   * 7.  cfn- Configuration File name. This is a ';' delimeted string of 
         config file contents
   * 8.  dev-: what device to use?
   * 9.  app-: Application arguments ..
   * 10. GO_FOR_IT_SIGNAL
   */ 
  private void pack(int nProcesses,int start_rank) {
    
    buffer = ByteBuffer.allocate(CONF_FILE_CONTENTS.getBytes().length+1000);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer (initial)" + buffer);
    }
    buffer.put("cpe-".getBytes());

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer (after putting url-) " + buffer);
    }
    buffer.putInt(urlArray.length);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer urlArray.length)" + buffer);
    }

    buffer.put(urlArray, 0, urlArray.length);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer urlArray itself " + buffer);
    }

    buffer.put("num-".getBytes());
	 
    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer " + buffer);
    }

    buffer.putInt(4);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer(after writing 4) " + buffer);
      logger.debug("nProcesses " + nProcesses);
    }

    buffer.putInt(nProcesses);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer(after nProcesses) " + buffer);
    }
	buffer.put("srk-".getBytes());
	 
    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer " + buffer);
    }
	buffer.putInt(4);

    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer(after writing 4) " + buffer);
      logger.debug("start_rank " + start_rank);
    }
    buffer.putInt(start_rank);
    if(DEBUG && logger.isDebugEnabled()) {
      logger.debug("buffer(after start_rank) " + buffer);
    }
    buffer.put("arg-".getBytes());
    buffer.putInt(jArgs.length); 
    for(int j=0 ; j<jArgs.length ; j++) {
      buffer.putInt(jArgs[j].getBytes().length);
      buffer.put(jArgs[j].getBytes(), 0, jArgs[j].getBytes().length);
    }

    if(wdir == null) { 
      wdir = System.getProperty("user.dir") ;
    }

    buffer.put("wdr-".getBytes());
    buffer.putInt(wdir.getBytes().length);
    buffer.put(wdir.getBytes(), 0, wdir.getBytes().length); 
    
    buffer.put("cls-".getBytes());
    buffer.putInt(className.getBytes().length);
    buffer.put(className.getBytes(), 0, className.getBytes().length); 

    /* these are contents of the config file stored in a single String */
    buffer.put("cfn-".getBytes());
    buffer.putInt(CONF_FILE_CONTENTS.getBytes().length);
    buffer.put(CONF_FILE_CONTENTS.getBytes(), 0, 
                                       CONF_FILE_CONTENTS.getBytes().length); 

    buffer.put("dev-".getBytes());
    buffer.putInt(deviceName.getBytes().length);
    buffer.put(deviceName.getBytes(), 0, deviceName.getBytes().length); 
	    
    buffer.put("app-".getBytes());
    buffer.putInt(aArgs.length); 

    for(int j=0 ; j<aArgs.length ; j++) {
      buffer.putInt(aArgs[j].getBytes().length);
      buffer.put(aArgs[j].getBytes(), 0, aArgs[j].getBytes().length);
    }

    buffer.put("*GO*".getBytes(), 0, "*GO*".getBytes().length);

    buffer.flip();
  }

  private void createLogger(String[] args) throws MPJRuntimeException {
  
    if(DEBUG && logger == null) {

      DailyRollingFileAppender fileAppender = null ;

      try {
	fileAppender = new DailyRollingFileAppender(
			    new PatternLayout(
			    " %-5p %c %x - %m\n" ),
			    mpjHomeDir+"/logs/mpjrun.log",
			    "yyyy-MM-dd-a" );

	Logger rootLogger = Logger.getRootLogger() ;
	rootLogger.addAppender( fileAppender);
	LoggerRepository rep =  rootLogger.getLoggerRepository() ;
	rootLogger.setLevel ((Level) Level.ALL );
	//rep.setThreshold((Level) Level.OFF ) ;
	logger = Logger.getLogger( "runtime" );
      }
      catch(Exception e) {
	throw new MPJRuntimeException(e) ;
      }
    }  
  }

  private void printUsage() { 
    System.out.println(   
      "mpjrun.[bat/sh] [options] class [args...]"+
      "\n                (to execute a class)"+
      "\nmpjrun.[bat/sh] [options] -jar jarfile [args...]"+
      "\n                (to execute a jar file)"+
      "\n\nwhere options include:"+
      "\n   -np val            -- <# of cores>"+ 
      "\n   -dev val           -- multicore"+
      "\n   -dport val         -- <read from wrapper.conf>"+ 
      "\n   -wdir val          -- $MPJ_HOME/bin"+ 
      "\n   -mpjport val       -- 20000"+  
      "\n   -mxboardnum val    -- 0"+  
      "\n   -headnodeip val    -- ..."+
      "\n   -psl val           -- 128Kbytes"+ 
      "\n   -machinesfile val  -- machines"+ 
      "\n   -h                 -- print this usage information"+ 
      "\n   ...any JVM arguments..."+
 "\n Note: Value on the right in front of each option is the default value"+ 
 "\n Note: 'MPJ_HOME' variable must be set");

  }
  

  /**
   * Parses the input ...
   */
  private void processInput(String args[]) {

    if (args.length < 1) {
      printUsage() ;
      System.exit(0);  
    }
 
    boolean append = false;
    boolean parallelProgramNotYetEncountered = true ; 
    
    for (int i = 0; i < args.length; i++) {

      if(args[i].equals("-np")) {

        try {  
          nprocs = new Integer(args[i+1]).intValue();
	} 
	catch(NumberFormatException e) {
	  nprocs = Runtime.getRuntime().availableProcessors();
	}

        i++;
      }

      else if(args[i].equals("-h")) {
        printUsage();
        System.exit(0); 
      }
      
      else if (args[i].equals("-dport")) {
        D_SER_PORT = new Integer(args[i+1]).intValue();
        i++;
      }

      else if (args[i].equals("-headnodeip")) {
	hostIP = args[i+1] ;
	i++;
      }
      
      else if (args[i].equals("-dev")) {
        deviceName = args[i+1];
        i++;
	if(!(deviceName.equals("niodev") || deviceName.equals("mxdev") ||
	                    deviceName.equals("multicore"))){
	  System.out.println("MPJ Express currently does not support the <"+
	                                   deviceName+"> device.");
          System.out.println("Possible options are niodev, mxdev, and "+
	                               "multicore devices.");
	  System.out.println("exiting ...");
	  System.exit(0); 
	}
      } 

      else if (args[i].equals("-machinesfile")) {
        machinesFile = args[i+1];
        i++;
      }

      else if (args[i].equals("-wdir")) {
        wdir = args[i+1];
        i++;
      }

      else if(args[i].equals("-psl")) {
        psl = new Integer(args[i+1]).intValue();
        i++;
      }
      
      else if (args[i].equals("-mpjport")) {
        MPJ_SERVER_PORT = new Integer(args[i+1]).intValue();
        i++;
      }
      
      else if (args[i].equals("-mxboardnum")) {
        mxBoardNum = new Integer(args[i+1]).intValue();
        i++;
      }
      
      else if (args[i].equals("-cp") | args[i].equals("-classpath")) {
        jvmArgs.add("-cp");
	jvmArgs.add(args[i+1]);
        i++;
      }
      
      else if (args[i].equals("-sport")) {
        S_PORT = new Integer(args[i+1]).intValue();
        i++;
      }
      
      else if(args[i].equals("-jar")) {
        File tFile = new File(args[i+1]);
	String absJarPath = tFile.getAbsolutePath();
	
	if(tFile.exists()) {
          applicationClassPathEntry = new String(absJarPath) ; 

          try { 
            JarFile jarFile = new JarFile(absJarPath) ;
            Attributes attr = jarFile.getManifest().getMainAttributes();
            className = attr.getValue(Attributes.Name.MAIN_CLASS);
          } catch(IOException ioe) { 
            ioe.printStackTrace() ; 
          } 
	  parallelProgramNotYetEncountered = false ; 
	  i++;
	}
	else {
          throw new MPJRuntimeException("mpjrun cannot find the jar file <"+
			  args[i+1]+">. Make sure this is the right path.");	
	}
	
      }

      else {
	      
        //these are JVM options .. 
        if(parallelProgramNotYetEncountered) {
          if(args[i].startsWith("-")) { 		
	    jvmArgs.add(args[i]); 
	  }
          else {
            //This code takes care of executing class files directly ....
            //although does not look like it ....
            applicationClassPathEntry = System.getProperty("user.dir");	      
 	    className = args[i];
	    parallelProgramNotYetEncountered = false ; 
          }
	}
	
        //these have to be app arguments ...		
	else {
          appArgs.add(args[i]);		
	}

      }

    }

    jArgs = jvmArgs.toArray(new String[0]);
    aArgs = appArgs.toArray(new String[0]);

    if(DEBUG && logger.isDebugEnabled()) {

      logger.debug("###########################"); 	    
      logger.debug("-dport: <"+D_SER_PORT+">");
      logger.debug("-mpjport: <"+MPJ_SERVER_PORT+">");
      logger.debug("-sport: <"+S_PORT+">");
      logger.debug("-np: <"+nprocs+">");
      logger.debug("$MPJ_HOME: <"+mpjHomeDir+">");
      logger.debug("-dir: <"+wdir+">"); 
      logger.debug("-dev: <"+deviceName+">");
      logger.debug("-psl: <"+psl+">");
      logger.debug("jvmArgs.length: <"+jArgs.length+">");
      logger.debug("className : <"+className+">");
      logger.debug("applicationClassPathEntry : <"+
                                                applicationClassPathEntry+">");
      

      for(int i=0; i<jArgs.length ; i++) {
	  if(DEBUG && logger.isDebugEnabled())
        logger.debug(" jvmArgs["+i+"]: <"+jArgs[i]+">");	      
      }
      if(DEBUG && logger.isDebugEnabled())
      logger.debug("appArgs.length: <"+aArgs.length+">");

      for(int i=0; i<aArgs.length ; i++) {
	  if(DEBUG && logger.isDebugEnabled())
        logger.debug(" appArgs["+i+"]: <"+aArgs[i]+">");	      
      }
      
      if(DEBUG && logger.isDebugEnabled())
      logger.debug("###########################"); 	    
    }
  }

  private synchronized void Wait() throws Exception {
    if (wait) {
	if(DEBUG && logger.isDebugEnabled())
      logger.debug("Waiting ...");
      this.wait();
	  if(DEBUG && logger.isDebugEnabled())
      logger.debug("Unwaiting ...");
    }

    wait = true;
  }

  private synchronized void Notify() {
  if(DEBUG && logger.isDebugEnabled())
    logger.debug("Notifying ..."); 	
    this.notify();
    wait = false;
  }

  private void assignTasks() throws Exception {
	  
    int rank = 0;

    int noOfMachines = machineVector.size();

    CONF_FILE_CONTENTS += ";" + "# Number of Processes" ;
    CONF_FILE_CONTENTS += ";" + nprocs ;
    CONF_FILE_CONTENTS += ";" + "# Protocol Switch Limit" ;
    CONF_FILE_CONTENTS += ";" + psl;
    CONF_FILE_CONTENTS += ";" + "# Entry, HOST_NAME/IP@SERVERPORT@RANK" ;

    /* number of requested parallel processes are less than or equal
       to compute nodes */ 
    if (nprocs <= noOfMachines) {

      if(DEBUG && logger.isDebugEnabled()) { 
        logger.debug("Processes Requested " + nprocs +
                  " are less than than machines " + noOfMachines);
        logger.debug("Adding 1 processes to the first " + nprocs +
                  " items");
      }

      for (int i = 0; i < nprocs; i++) {
        procsPerMachineTable.put( (String) machineVector.get(i),
                                 new Integer(1));
	 
	if(deviceName.equals("niodev")) { 
          CONF_FILE_CONTENTS += ";"+(String) machineVector.get(i)+"@"
                                    + MPJ_SERVER_PORT + "@" + (rank++) ;
	} else if(deviceName.equals("mxdev")) { 
          CONF_FILE_CONTENTS += ";"+(String) machineVector.get(i)+"@"
                                    + mxBoardNum + "@" + (rank++) ;
	} 
	
        if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	}
      }

    /* number of processes are greater than compute nodes available. we'll 
       start more than one process on compute nodes to deal with this */
    } else if (nprocs > noOfMachines) {

      if(DEBUG && logger.isDebugEnabled()) { 
          logger.debug("Processes Requested " + nprocs +
                       " are greater than than machines " + noOfMachines);
      }

      int divisor = nprocs / noOfMachines;
      if(DEBUG && logger.isDebugEnabled()) { 
	  logger.debug("divisor " + divisor);
      }
      int remainder = nprocs % noOfMachines;

      if(DEBUG && logger.isDebugEnabled()) { 
	  logger.debug("remainder " + remainder);
      }

      for (int i = 0; i < noOfMachines; i++) {
	      
        if (i < remainder) {
		
          procsPerMachineTable.put( (String) machineVector.get(i),
                                   new Integer(divisor + 1));
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	  }
	  
          for (int j = 0; j < (divisor + 1); j++) {
            if(deviceName.equals("niodev")) { 		  
              CONF_FILE_CONTENTS += ";"+(String) machineVector.get(i)+"@"+
                                (MPJ_SERVER_PORT + (j * 2)) + "@" + (rank++) ;
	    } else if(deviceName.equals("mxdev")) { 
              CONF_FILE_CONTENTS += ";" +  (String) machineVector.get(i) + "@" +
                                (mxBoardNum+j) + "@" + (rank++) ;
	    }
          }
        } else if (divisor > 0) {
          procsPerMachineTable.put( (String) machineVector.get(i),
                                   new Integer(divisor));
	  
          if(DEBUG && logger.isDebugEnabled()) { 
            logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	  }

          for (int j = 0; j < divisor; j++) {
            if(deviceName.equals("niodev")) { 		  
              CONF_FILE_CONTENTS += ";" + (String) machineVector.get(i) + "@" +
                             (MPJ_SERVER_PORT + (j * 2)) + "@" + (rank++) ;
	    } else if(deviceName.equals("mxdev")) { 
              CONF_FILE_CONTENTS += ";" + (String) machineVector.get(i) + "@" +
                                          (mxBoardNum+j) + "@" + (rank++) ;
	    }
          }
        }
      }
    } 
  }

  private void machinesSanityCheck() throws Exception {
	  
    for(int i=0 ; i<machineVector.size() ; i++) {
	    
      String host = (String) machineVector.get(i) ;

      try {
        InetAddress add = InetAddress.getByName(host);
      } catch( Exception e) {
        throw new MPJRuntimeException (e);	      
      }
      
    }

  }

  /* assume 'machines'is in the current directory */
  public void readMachineFile() throws Exception {

    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new FileReader( machinesFile ));
    }
    catch (FileNotFoundException fnfe) {
      throw new MPJRuntimeException ( "<"+ machinesFile + "> file cannot "+
                            " be found." +
                            " The starter module assumes "+
                            "it to be in the current directory.");
    }

    boolean loop = true;
    String line = null;
    int machineCount = 0 ; 

    while (machineCount < nprocs) {

      line = reader.readLine();

      if(DEBUG && logger.isDebugEnabled()) {
        logger.debug("line <" + line + ">");
      }

      if(line == null) { 
        break ; 
      }

      if (line.startsWith("#") || line.equals("") ||
          (machineVector.size() == nprocs)) {
        //loop = false;
        continue ;
      }

      machineCount ++ ;

      line = line.trim();

      InetAddress address = InetAddress.getByName(line);
      String addressT = address.getHostAddress();
      String nameT = address.getHostName();

      if(DEBUG && logger.isDebugEnabled()) {
        logger.debug("nameT " + nameT);
        logger.debug("addressT " + addressT);
      } 
     
      boolean alreadyPresent = false;
      
      for(int i=0 ; i<machineVector.size() ; i++) {
        String machine = (String) machineVector.get(i); 

        if(machine.equals(nameT) || machine.equals(addressT)) {  
           alreadyPresent = true;
           break ;
        }
      }

      if(!alreadyPresent) { 

        //machineVector.add(addressT);
        machineVector.add(nameT);

        if(DEBUG && logger.isDebugEnabled()) {
          logger.debug("Line " + line.trim() +
                    " added to vector " + machineVector);
        }
      }
    }//end while.
  }

  private static int getPortFromWrapper() {

    int port = 0;
    FileInputStream in = null;
    DataInputStream din = null;
    BufferedReader reader = null;
    String line = "";

    try {

      String path = System.getenv("MPJ_HOME")+"/conf/wrapper.conf";
      in = new FileInputStream(path);
      din = new DataInputStream(in);
      reader = new BufferedReader(new InputStreamReader(din));

      while ((line = reader.readLine()) != null)   {
        if(line.startsWith("wrapper.app.parameter.2")) {
          String trimmedLine=line.replaceAll("\\s+", "");
          port = Integer.parseInt(trimmedLine.substring(24));
          break;
        }
      }

      in.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return port;

  }

  private void clientSocketInit() throws Exception {
      	  
    SocketChannel[] clientChannels = new SocketChannel[machineVector.size()];
    for (int i = 0; i < machineVector.size(); i++) {
      boolean connected = false ; 	    
      String daemon = (String) machineVector.get(i);
      try {
        clientChannels[i] = SocketChannel.open();
        clientChannels[i].configureBlocking(true);
		if(DEBUG && logger.isDebugEnabled())
		{
        logger.debug("Connecting to " + daemon + "@" + D_SER_PORT);
		}      
	  connected = clientChannels[i].connect(
			new InetSocketAddress(daemon, D_SER_PORT));

	if(!connected) {
          throw new MPJRuntimeException("Cannot connect to the daemon "+
			  "at machine <"+daemon+"> and port <"+
			  D_SER_PORT+">."+
			  "Please make sure that the machine is reachable "+
			  "and running the daemon in 'sane' state"); 
	}

	doConnect(clientChannels[i]); 
      }
      catch(IOException ioe) {

	System.out.println(" IOException in doConnect");
        throw new MPJRuntimeException("Cannot connect to the daemon "+
			"at machine <"+daemon+"> and port <"+
			D_SER_PORT+">."+
			"Please make sure that the machine is reachable "+
			"and running the daemon in 'sane' state"); 
      }
      catch (Exception ccn1) {
	  System.out.println(" rest of the exceptions ");
        throw ccn1;
      }
    }
  }

  /**
   * This method cleans up the device environments, closes the selectors,
   * serverSocket, and all the other socketChannels
   */
  public void finish() {
   if(DEBUG && logger.isDebugEnabled())
	{
    logger.debug("\n---finish---");
	}
    try {
      if(DEBUG && logger.isDebugEnabled())
	 {
      logger.debug("Waking up the selector");
      
	 }
	 selector.wakeup();
      selectorFlag = false;
	if(DEBUG && logger.isDebugEnabled())
	 {
      logger.debug("Closing the selector");
      }
	  selector.close();

      SocketChannel peerChannel = null;

      for (int i = 0; i < peerChannels.size(); i++) {
        peerChannel = peerChannels.get(i);
	if(DEBUG && logger.isDebugEnabled())
	 {       
	   logger.debug("Closing the channel " + peerChannel);
	 }
        if (peerChannel.isOpen()) {
          peerChannel.close();
        }


      }

      peerChannel = null;
    }
    catch (Exception e) {
      //e.printStackTrace();
    }
  }

  private void doConnect(SocketChannel peerChannel) {
  if(DEBUG && logger.isDebugEnabled())
    logger.debug("---doConnect---");
    try {
	if(DEBUG && logger.isDebugEnabled())
      logger.debug("Configuring it to be non-blocking");
      peerChannel.configureBlocking(false);
    }
    catch (IOException ioe) {
	if(DEBUG && logger.isDebugEnabled())
      logger.debug("Closed Channel Exception in doConnect");
      System.exit(0);
    }

    try {
	if(DEBUG && logger.isDebugEnabled())
      logger.debug("Registering for OP_READ & OP_WRITE event");
      peerChannel.register(selector,
                           SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    catch (ClosedChannelException cce) {
	if(DEBUG && logger.isDebugEnabled())
      logger.debug("Closed Channel Exception in doConnect");
      System.exit(0);
    }

    try {
      peerChannel.socket().setTcpNoDelay(true);
    }
    catch (Exception e) {}
    peerChannels.add(peerChannel);
	if(DEBUG && logger.isDebugEnabled())
	{
    logger.debug("Adding the channel " + peerChannel + " to " + peerChannels);
    logger.debug("Size of Peer Channels vector " + peerChannels.size());
    }
    peerChannel = null;
    if (peerChannels.size() == machineVector.size()) {
      Notify();
    }
  }
  
  /**
   * Entry point to the class 
   */
  public static void main(String args[]) throws Exception {

    try {
      MPJRun client = new MPJRun(args);
    }
    catch (Exception e) {
      throw e;
    }

  }

  private void addShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          for (int j = 0; j < peerChannels.size(); j++) {
            SocketChannel socketChannel = null;
            socketChannel = peerChannels.get(j);
            buffer.clear();
            buffer.put( (new String("kill")).getBytes());
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
          }

        }
        catch(Exception e){
        }
      }
    });
  }

  Runnable selectorThread = new Runnable() {

    /* This is selector thread */
    public void run() {

      if(DEBUG && logger.isDebugEnabled())     
        logger.debug("selector Thread started ");

      Set readyKeys = null;
      Iterator readyItor = null;
      SelectionKey key = null;
      SelectableChannel keyChannel = null;
      SocketChannel socketChannel = null;
      ByteBuffer lilBuffer = ByteBuffer.allocateDirect(4);
      ByteBuffer bigBuffer = ByteBuffer.allocateDirect(10000);

      try {
        while (selector.select() > -1 && selectorFlag == true) {

          readyKeys = selector.selectedKeys();
          readyItor = readyKeys.iterator();

          while (readyItor.hasNext()) {

            key = (SelectionKey) readyItor.next();
            readyItor.remove();
            keyChannel = (SelectableChannel) key.channel();
			if(DEBUG && logger.isDebugEnabled())
            logger.debug("\n---selector EVENT---");

            if (key.isAcceptable()) {
              //doAccept(keyChannel);
			  if(DEBUG && logger.isDebugEnabled())
              logger.debug("ACCEPT_EVENT");
            }

            else if (key.isConnectable()) {
				if(DEBUG && logger.isDebugEnabled())
              logger.debug("CONNECT_EVENT");
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

              doConnect(socketChannel);
            }

            else if (key.isReadable()) { 
              //if(DEBUG && logger.isDebugEnabled())
              //logger.debug("READ_EVENT");
              socketChannel = (SocketChannel) keyChannel;
              int read = socketChannel.read(bigBuffer);  

              /* 
               * It would be ideal if this piece of code is called ...
               * but it appears ..its never callled ..maybe the behaviour
               * of closing down that we saw was Linux dependant ????
               */ 

              if (read == -1) {
                if(DEBUG && logger.isDebugEnabled())
	          logger.debug("END_OF_STREAM signal at starter from "+
                                                "channel "+socketChannel) ;  
                streamEndedCount ++ ;  

                if (streamEndedCount == machineVector.size()) {
				if(DEBUG && logger.isDebugEnabled())
				{
                  logger.debug("The starter has received "+ 
                               machineVector.size() +"signals"); 
                  logger.debug("This means its time to exit"); 
				}                 
		  Notify();
                }
                
              } 

              bigBuffer.flip();

              if(read == -1) { 
                System.exit(0);  
              }

              byte[] tempArray = new byte[read];
              bigBuffer.get(tempArray, 0, read);
              String line = new String(tempArray);
              bigBuffer.clear();

              if (line.endsWith("EXIT")) {
                endCount++;
                if(DEBUG && logger.isDebugEnabled())
				{
				logger.debug("endCount " + endCount);
                logger.debug("machineVector.size() " + machineVector.size());
				}
                if (endCount == machineVector.size()) {
				if(DEBUG && logger.isDebugEnabled())
				 logger.debug("Notify and exit"); 
                  Notify();
                }
              } 
	      else {
                System.out.print(line);
              }

            } //end if key.isReadable()

            else if (key.isWritable()) {
			if(DEBUG && logger.isDebugEnabled())
              logger.debug(
                  "In, WRITABLE, so changing the interestOps to READ_ONLY");
              key.interestOps(SelectionKey.OP_READ);
            }
          }
        }
      }
      catch (Exception ioe1) {
	  if(DEBUG && logger.isDebugEnabled())
        logger.debug("Exception in selector thread ");
        ioe1.printStackTrace();
        System.exit(0);
      }
	  if(DEBUG && logger.isDebugEnabled())
      logger.debug("Thread getting out");
    }
  };
}
