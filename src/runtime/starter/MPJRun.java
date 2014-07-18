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
 * Author       : Aamir Shafi, Bryan Carpenter,Khurram Shahzad, Mohsan Jameel, Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.35 $
 * Updated      : $Date: Wed Nov  5 20:55:53 EST 2013$
 */

package runtime.starter;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggerRepository;

import runtime.common.IOHelper;
import runtime.common.MPJProcessTicket;
import runtime.common.MPJRuntimeException;
import runtime.common.MPJUtil;
import runtime.common.RTConstants;

public class MPJRun {

  final String DEFAULT_MACHINES_FILE_NAME = "machines";
  final int DEFAULT_PROTOCOL_SWITCH_LIMIT = 128 * 1024; // 128K

  private String CONF_FILE_CONTENTS = "#temp line";
  private int mxBoardNum = 0;
  private int D_SER_PORT = 0;
  private int DEBUG_PORT = 0;
  private int portManagerPort = 0;

  String machinesFile = DEFAULT_MACHINES_FILE_NAME;
  private int psl = DEFAULT_PROTOCOL_SWITCH_LIMIT;

  ArrayList<String> jvmArgs = new ArrayList<String>();
  ArrayList<String> appArgs = new ArrayList<String>();
  String[] jArgs = null;
  String[] aArgs = null;
  static Logger logger = null;
  private Vector<Socket> peerSockets;

  private ArrayList<String> machineList = new ArrayList<String>();
  int nprocs = Runtime.getRuntime().availableProcessors();
  String deviceName = "multicore";
  private String networkDevice = "niodev";

  static String mpjHomeDir = null;
  byte[] urlArray = null;
  Hashtable procsPerMachineTable = new Hashtable();
  int endCount = 0;
  int streamEndedCount = 0;
  String wdir;
  String className = null;
  String applicationClassPathEntry = null;

  private static String VERSION = "";
  private static int RUNNING_JAR_FILE = 2;
  private static int RUNNING_CLASS_FILE = 1;
  private boolean zippedSource = false;
  private String sourceFolder = "";
  int networkProcesscount = -1;

  private boolean ADEBUG = false;
  private boolean APROFILE = false;

  static final boolean DEBUG = true;
  private String logLevel = "OFF";

  /**
   * Every thing is being inside this constructor :-)
   */
  public MPJRun(String args[]) throws Exception {

    java.util.logging.Logger logger1 = java.util.logging.Logger.getLogger("");

    // remove all existing log handlers: remove the ERR handler
    for (java.util.logging.Handler h : logger1.getHandlers()) {
      logger1.removeHandler(h);
    }

    Map<String, String> map = System.getenv();
    try {
      mpjHomeDir = map.get("MPJ_HOME");
      RTConstants.MPJ_HOME_DIR = mpjHomeDir;
      if (mpjHomeDir == null) {
	throw new Exception("MPJ_HOME environment variable not set!!!");
      }
    }
    catch (Exception exc) {
      System.out.println("Error: " + exc.getMessage());
      exc.printStackTrace();
      return;
    }
    readValuesFromMPJExpressConf();
    createLogger(args);

    if (DEBUG && logger.isDebugEnabled()) {
      logger.info(" --MPJRun invoked--");
      logger.info(" adding shutdown hook thread");
    }

    if (DEBUG && logger.isDebugEnabled()) {
      logger.info("processInput called ...");
    }

    processInput(args);

    /* the code is running in the multicore configuration */
    if (deviceName.equals("multicore")) {

      System.out.println("MPJ Express (" + VERSION + ") is started in the "
	  + "multicore configuration");
      if (DEBUG && logger.isDebugEnabled()) {
	logger.info("className " + className);
      }

      int jarOrClass = (applicationClassPathEntry.endsWith(".jar") ? RUNNING_JAR_FILE
	  : RUNNING_CLASS_FILE);

      MulticoreDaemon multicoreDaemon = new MulticoreDaemon(className,
	  applicationClassPathEntry, jarOrClass, nprocs, wdir, jvmArgs,
	  appArgs, mpjHomeDir, ADEBUG, APROFILE, DEBUG_PORT);
      return;

    } else { /* cluster configuration */
      System.out.println("MPJ Express (" + VERSION + ") is started in the "
	  + "cluster configuration with " + deviceName);
    }

    machineList = MPJUtil.readMachineFile(machinesFile);
    for (int i = machineList.size(); i > nprocs; i--) {
      machineList.remove(i - 1);
    }

    machinesSanityCheck();
    // Changed to incorporate hybrid device configuration
    if (deviceName.equals("hybdev"))
      assignTasksHyb();
    else
      assignTasks();

    if (ADEBUG) {
      writeFile(CONF_FILE_CONTENTS + "\n");
    }

    urlArray = applicationClassPathEntry.getBytes();
    peerSockets = new Vector<Socket>();
    clientSocketInit();
    int peersStartingRank = 0;

    for (int j = 0; j < peerSockets.size(); j++) {

      Socket peerSock = peerSockets.get(j);

      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("procsPerMachineTable " + procsPerMachineTable);
      }

      /*
       * FIXME: should we not be checking all IP addresses of remote machine?
       * Does it make sense?
       */

      String hAddress = peerSock.getInetAddress().getHostAddress();
      String hName = peerSock.getInetAddress().getHostName();

      Integer nProcessesInt = ((Integer) procsPerMachineTable.get(hName));

      if (nProcessesInt == null) {
	nProcessesInt = ((Integer) procsPerMachineTable.get(hAddress));
      }

      int nProcesses = nProcessesInt.intValue();

      if (deviceName.equals("hybdev")) {
	pack(nProcesses, j, peerSock); // starting NETID of hybrid
	// device should be adjusted
	// according to node
	// (NioProcessCount,
	// StartingRank)
      } else {

	pack(nProcesses, peersStartingRank, peerSock);
	peersStartingRank += nProcesses;
      }
      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("Sending to " + peerSock);
      }
    }

    if (DEBUG && logger.isDebugEnabled()) {
      logger.debug("procsPerMachineTable " + procsPerMachineTable);
    }

  }

  /**
   * Parses the input ...
   */
  private void processInput(String args[]) {

    if (args.length < 1) {
      printUsage();
      System.exit(0);
    }

    boolean parallelProgramNotYetEncountered = true;

    for (int i = 0; i < args.length; i++) {

      if (args[i].equals("-np")) {

	try {
	  nprocs = new Integer(args[i + 1]).intValue();
	  if (nprocs < 1) {
	    System.out
		.println("Number of Processes should be equal to or greater than 1");
	    System.out.println("exiting ...");
	    System.exit(0);
	  }
	}
	catch (NumberFormatException e) {
	  nprocs = Runtime.getRuntime().availableProcessors();
	}

	i++;
      }

      else if (args[i].equals("-h")) {
	printUsage();
	System.exit(0);
      }

      else if (args[i].equals("-dport")) {
	D_SER_PORT = new Integer(args[i + 1]).intValue();
	i++;
      }

      else if (args[i].equals("-dev")) {
	deviceName = args[i + 1];
	i++;
	if (!(deviceName.equals("niodev") || deviceName.equals("hybdev")
	    || deviceName.equals("mxdev") || deviceName.equals("multicore"))) {
	  System.out.println("MPJ Express currently does not support the <"
	      + deviceName + "> device.");
	  System.out
	      .println("Possible options are niodev, hybdev, mxdev, native, and "
		  + "multicore devices.");
	  System.out.println("exiting ...");
	  System.exit(0);
	}
      }

      else if (args[i].equals("-machinesfile")) {
	machinesFile = args[i + 1];
	i++;
      }

      else if (args[i].equals("-wdir")) {
	wdir = args[i + 1];
	i++;
      }

      else if (args[i].equals("-psl")) {
	psl = new Integer(args[i + 1]).intValue();
	i++;
      }

      else if (args[i].equals("-mxboardnum")) {
	mxBoardNum = new Integer(args[i + 1]).intValue();
	i++;
      }

      else if (args[i].equals("-cp") | args[i].equals("-classpath")) {
	jvmArgs.add("-cp");
	jvmArgs.add(args[i + 1]);
	i++;
      } else if (args[i].equals("-jar")) {
	File tFile = new File(args[i + 1]);
	String absJarPath = tFile.getAbsolutePath();

	if (tFile.exists()) {
	  applicationClassPathEntry = new String(absJarPath);

	  try {
	    JarFile jarFile = new JarFile(absJarPath);
	    Attributes attr = jarFile.getManifest().getMainAttributes();
	    className = attr.getValue(Attributes.Name.MAIN_CLASS);
	  }
	  catch (IOException ioe) {
	    ioe.printStackTrace();
	  }
	  parallelProgramNotYetEncountered = false;
	  i++;
	} else {
	  throw new MPJRuntimeException("mpjrun cannot find the jar file <"
	      + args[i + 1] + ">. Make sure this is the right path.");
	}

      } else if (args[i].equals("-src")) {
	this.zippedSource = true;
      } else if (args[i].equals("-debug")) {
	DEBUG_PORT = new Integer(args[i + 1]).intValue();
	i++;
	ADEBUG = true;
      } else if (args[i].equals("-profile")) {
	APROFILE = true;
      } else {
	// these are JVM options ..
	if (parallelProgramNotYetEncountered) {
	  if (args[i].startsWith("-")) {
	    jvmArgs.add(args[i]);
	  } else {
	    // This code takes care of executing class files
	    // directly ....
	    // although does not look like it ....
	    applicationClassPathEntry = System.getProperty("user.dir");
	    className = args[i];
	    parallelProgramNotYetEncountered = false;
	  }
	}

	// these have to be app arguments ...
	else {
	  appArgs.add(args[i]);
	}

      }

    }

    jArgs = jvmArgs.toArray(new String[0]);
    aArgs = appArgs.toArray(new String[0]);

    if (DEBUG && logger.isDebugEnabled()) {

      logger.debug("###########################");
      logger.debug("-dport: <" + D_SER_PORT + ">");
      logger.debug("-np: <" + nprocs + ">");
      logger.debug("$MPJ_HOME: <" + mpjHomeDir + ">");
      logger.debug("-dir: <" + wdir + ">");
      logger.debug("-dev: <" + deviceName + ">");
      logger.debug("-psl: <" + psl + ">");
      logger.debug("jvmArgs.length: <" + jArgs.length + ">");
      logger.debug("className : <" + className + ">");
      logger.debug("applicationClassPathEntry : <" + applicationClassPathEntry
	  + ">");

      for (int i = 0; i < jArgs.length; i++) {
	if (DEBUG && logger.isDebugEnabled())
	  logger.debug(" jvmArgs[" + i + "]: <" + jArgs[i] + ">");
      }
      if (DEBUG && logger.isDebugEnabled())
	logger.debug("appArgs.length: <" + aArgs.length + ">");

      for (int i = 0; i < aArgs.length; i++) {
	if (DEBUG && logger.isDebugEnabled())
	  logger.debug(" appArgs[" + i + "]: <" + aArgs[i] + ">");
      }

      if (DEBUG && logger.isDebugEnabled())
	logger.debug("###########################");
    }
  }

  /*
   * 1. Application Classpath Entry (urlArray). This is a String classpath entry
   * which will be appended by the MPJ Express daemon before starting a user
   * process (JVM). In the case of JAR file, it's the absolute path and name. In
   * the case of a class file, its the name of the working directory where
   * mpjrun command was launched. 2. nProcs- [# of processes] to be started by a
   * particular MPJ Express daemon. 3. start_rank [starting #(rank) of process]
   * to be started by a particular MPJ Express daemon. 4. jvmArgs- args to JVM
   * 5. wdir Working Directory 6. className- Classname to be executed. In the
   * case of JAR file, this name is taken from the manifest file. In the case of
   * class file, the class name is specified on the command line by the user. 7.
   * CONF_FILE_CONTENTS- Configuration File name. This is a ';' delimeted string
   * of config file contents 8. deviceName-: what device to use? 9. appArgs-:
   * Application arguments .. 10. networkDevice- niodev in case of Hybdrid 11.
   * ADEBUG- Flag for launching application in debug mode 12. APROFILE- Flag for
   * launching application in Profiling mode
   */
  private void pack(int nProcesses, int start_rank, Socket sockClient) {

    if (wdir == null) {
      wdir = System.getProperty("user.dir");
    }

    MPJProcessTicket ticket = new MPJProcessTicket();
    ticket.setMpjHomeDir(mpjHomeDir);
    ticket.setClassPath(new String(urlArray));
    ticket.setProcessCount(nProcesses);
    ticket.setStartingRank(start_rank);
    ticket.setWorkingDirectory(wdir);
    ticket.setUserID(System.getProperty("user.name"));
    if (this.zippedSource) {
      String zipFileName = UUID.randomUUID() + ".zip";
      this.sourceFolder = wdir;
      IOHelper.zipFolder(this.sourceFolder, zipFileName);
      byte[] zipContents = IOHelper.ReadBinaryFile(zipFileName);
      String encodedString = Base64.encodeBase64String(zipContents);
      ticket.setSourceCode(encodedString);
      IOHelper.deleteFile(zipFileName);
      ticket.setZippedSource(true);
    }
    ticket.setMainClass(className);
    ticket.setConfFileContents(CONF_FILE_CONTENTS);
    ticket.setDeviceName(deviceName);
    IOMessagesThread ioMessages = new IOMessagesThread(sockClient);
    ioMessages.start();
    ArrayList<String> jvmArgs = new ArrayList<String>();
    for (int j = 0; j < jArgs.length; j++) {
      jvmArgs.add(jArgs[j]);
    }
    ticket.setJvmArgs(jvmArgs);

    ArrayList<String> appArgs = new ArrayList<String>();
    for (int j = 0; j < aArgs.length; j++) {
      appArgs.add(aArgs[j]);
    }
    ticket.setAppArgs(appArgs);

    if (deviceName.equals("hybdev")) {
      ticket.setNetworkProcessCount(networkProcesscount);
      ticket.setTotalProcessCount(nprocs);
      ticket.setNetworkDevice(networkDevice);
    }

    if (ADEBUG) {
      ticket.setDebug(true);
      ticket.setDebugPort(DEBUG_PORT);
    }

    if (APROFILE) {
      ticket.setProfiler(true);
    }
    String ticketString = ticket.ToXML().toXmlString();
    OutputStream outToServer = null;
    try {
      outToServer = sockClient.getOutputStream();
    }
    catch (IOException e) {
      logger.info(" Unable to get deamon stream-");
      e.printStackTrace();
    }
    DataOutputStream out = new DataOutputStream(outToServer);

    try {
      int length = ticketString.getBytes().length;
      out.writeInt(length);
      if (DEBUG && logger.isDebugEnabled()) {
	logger.info("Machine Name: "
	    + sockClient.getInetAddress().getHostName() + " Startting Rank: "
	    + ticket.getStartingRank() + " Process Count: "
	    + ticket.getProcessCount());
      }
      out.write(ticketString.getBytes(), 0, length);
      out.flush();
    }
    catch (IOException e) {

      logger.info(" Unable to write on deamon stream-");
      e.printStackTrace();
    }
  }

  private void createLogger(String[] args) throws MPJRuntimeException {
    String userDir = System.getProperty("user.dir");
    if (DEBUG && logger == null) {

      DailyRollingFileAppender fileAppender = null;

      try {
	if (logLevel.toUpperCase().equals("DEBUG")) {
	  fileAppender = new DailyRollingFileAppender(new PatternLayout(
	      " %-5p %c %x - %m\n"), userDir + "/mpjrun.log", "yyyy-MM-dd-a");

	  Logger rootLogger = Logger.getRootLogger();
	  rootLogger.addAppender(fileAppender);
	  LoggerRepository rep = rootLogger.getLoggerRepository();
	  rootLogger.setLevel((Level) Level.ALL);
	}
	// rep.setThreshold((Level) Level.OFF ) ;
	logger = Logger.getLogger("runtime");
	logger.setLevel(Level.toLevel(logLevel.toUpperCase(), Level.OFF));
      }
      catch (Exception e) {
	throw new MPJRuntimeException(e);
      }
    }
  }

  private void printUsage() {
    System.out
	.println("MPJ Express version " + VERSION
	    + "\n\nmpjrun.[bat/sh] [options] class [args...]"
	    + "\n                (to execute a class)"
	    + "\nmpjrun.[bat/sh] [options] -jar jarfile [args...]"
	    + "\n                (to execute a jar file)"
	    + "\n\nwhere options include:"
	    + "\n   -np val            -- <# of cores>"
	    + "\n   -dev val           -- <multicore>"
	    + "\n   -dport val         -- <read from mpjexpress.conf>"
	    + "\n   -wdir val          -- $MPJ_HOME/bin"
	    + "\n   -mpjport val       -- Deprecated"
	    + "\n   -mxboardnum val    -- 0"
	    + "\n   -headnodeip val    -- ..."
	    + "\n   -psl val           -- 128Kbytes"
	    + "\n   -machinesfile val  -- machines"
	    + "\n   -debug val         -- 24500"
	    + "\n   -src val           -- false"
	    + "\n   -profile val       -- false"
	    + "\n   -h                 -- print this usage information"
	    + "\n   ...any JVM arguments..."
	    + "\n Note: Value on the right in front of each option is the default value"
	    + "\n Note: 'MPJ_HOME' variable must be set");

  }

  private void assignTasks() throws Exception {

    int rank = 0;

    int noOfMachines = machineList.size();

    CONF_FILE_CONTENTS += ";" + "# Number of Processes";
    CONF_FILE_CONTENTS += ";" + nprocs;
    CONF_FILE_CONTENTS += ";" + "# Protocol Switch Limit";
    CONF_FILE_CONTENTS += ";" + psl;
    CONF_FILE_CONTENTS += ";"
	+ "# Entry, HOST_NAME/IP@READPORT@WRITEPORT@RANK@DEBUGPORT";

    /*
     * number of requested parallel processes are less than or equal to compute
     * nodes
     */
    if (nprocs <= noOfMachines) {

      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("Processes Requested " + nprocs
	    + " are less than than machines " + noOfMachines);
	logger.debug("Adding 1 processes to the first " + nprocs + " items");
      }

      for (int i = 0; i < nprocs; i++) {
	procsPerMachineTable
	    .put(InetAddress.getByName((String) machineList.get(i))
		.getHostAddress(), new Integer(1));

	if (deviceName.equals("niodev")) {
	  Integer[] ports = getNextAvialablePorts((String) machineList.get(i));
	  int readPort = ports[0];
	  int writePort = ports[1];
	  CONF_FILE_CONTENTS += ";"
	      + InetAddress.getByName((String) machineList.get(i))
		  .getHostAddress() + "@" + readPort + "@" + writePort + "@"
	      + (rank++);

	} else if (deviceName.equals("mxdev")) {
	  CONF_FILE_CONTENTS += ";" + (String) machineList.get(i) + "@"
	      + mxBoardNum + "@" + (rank++);
	}
	CONF_FILE_CONTENTS += "@" + (DEBUG_PORT);

	if (DEBUG && logger.isDebugEnabled()) {
	  logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	}
      }

      /*
       * number of processes are greater than compute nodes available. we'll
       * start more than one process on compute nodes to deal with this
       */
    } else if (nprocs > noOfMachines) {

      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("Processes Requested " + nprocs
	    + " are greater than than machines " + noOfMachines);
      }

      int divisor = nprocs / noOfMachines;
      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("divisor " + divisor);
      }
      int remainder = nprocs % noOfMachines;

      if (DEBUG && logger.isDebugEnabled()) {
	logger.debug("remainder " + remainder);
      }

      for (int i = 0; i < noOfMachines; i++) {

	if (i < remainder) {

	  procsPerMachineTable.put(
	      InetAddress.getByName((String) machineList.get(i))
		  .getHostAddress(), new Integer(divisor + 1));
	  if (DEBUG && logger.isDebugEnabled()) {
	    logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	  }

	  for (int j = 0; j < (divisor + 1); j++) {

	    if (deviceName.equals("niodev")) {

	      Integer[] ports = getNextAvialablePorts((String) machineList
		  .get(i));
	      int readPort = ports[0];
	      int writePort = ports[1];

	      CONF_FILE_CONTENTS += ";"
		  + InetAddress.getByName((String) machineList.get(i))
		      .getHostAddress() + "@" + readPort + "@" + writePort
		  + "@" + (rank++);

	    } else if (deviceName.equals("mxdev")) {
	      CONF_FILE_CONTENTS += ";" + (String) machineList.get(i) + "@"
		  + (mxBoardNum + j) + "@" + (rank++);
	    }
	    CONF_FILE_CONTENTS += "@" + (DEBUG_PORT + j * 2);
	  }
	} else if (divisor > 0) {
	  procsPerMachineTable.put(
	      InetAddress.getByName((String) machineList.get(i))
		  .getHostAddress(), new Integer(divisor));

	  if (DEBUG && logger.isDebugEnabled()) {
	    logger.debug("procPerMachineTable==>" + procsPerMachineTable);
	  }

	  for (int j = 0; j < divisor; j++) {

	    if (deviceName.equals("niodev")) {
	      Integer[] ports = getNextAvialablePorts((String) machineList
		  .get(i));
	      int readPort = ports[0];
	      int writePort = ports[1];

	      CONF_FILE_CONTENTS += ";"
		  + InetAddress.getByName((String) machineList.get(i))
		      .getHostAddress() + "@" + readPort + "@" + writePort
		  + "@" + (rank++);
	    } else if (deviceName.equals("mxdev")) {
	      CONF_FILE_CONTENTS += ";" + (String) machineList.get(i) + "@"
		  + (mxBoardNum + j) + "@" + (rank++);
	    }
	    CONF_FILE_CONTENTS += "@" + (DEBUG_PORT + j * 2);
	  }
	}
      }
    }
  }

  // ________________ HD _________________________

  private void assignTasksHyb() throws Exception {

    int noOfMachines = machineList.size();
    networkProcesscount = -1;
    if (nprocs <= noOfMachines) {
      networkProcesscount = nprocs;
    } else { // when np is higher than the nodes available
      networkProcesscount = noOfMachines;
    }
    int netID = 0;
    CONF_FILE_CONTENTS += ";" + "# Number of NIO Processes";
    CONF_FILE_CONTENTS += ";" + networkProcesscount;
    CONF_FILE_CONTENTS += ";" + "# Protocol Switch Limit";
    CONF_FILE_CONTENTS += ";" + psl;
    CONF_FILE_CONTENTS += ";"
	+ "# Entry, HOST_NAME/IP@READPORT@WRITEPORT@NETID@DEBUGPORT";
    // One NIO Process per machine is being implemented, SMP Threads per
    // node will be decided in SMPDev
    for (int i = 0; i < networkProcesscount; i++) {
      procsPerMachineTable.put(
	  InetAddress.getByName((String) machineList.get(i)).getHostAddress(),
	  new Integer(1));
      Integer[] ports = getNextAvialablePorts((String) machineList.get(i));
      int readPort = ports[0];
      int writePort = ports[1];
      CONF_FILE_CONTENTS += ";"
	  + InetAddress.getByName((String) machineList.get(i)).getHostAddress()
	  + "@" + readPort + "@" + writePort + "@" + (netID++);
      CONF_FILE_CONTENTS += "@" + (DEBUG_PORT);
    }

    if (DEBUG && logger.isDebugEnabled()) {
      logger.debug("procPerMachineTable==>" + procsPerMachineTable);
    }

  }

  private Integer[] getNextAvialablePorts(String machineName) {

    Integer[] ports = new Integer[2];

    Socket portClient = null;
    try {
      portClient = new Socket(machineName, portManagerPort);
      OutputStream outToServer = portClient.getOutputStream();
      DataOutputStream out = new DataOutputStream(outToServer);
      out.writeInt(1);
      out.flush();
      DataInputStream din = new DataInputStream(portClient.getInputStream());
      ports[0] = din.readInt();
      ports[1] = din.readInt();
      out.writeInt(2);
      out.flush();

    }
    catch (IOException e) {
      System.out.println("Cannot connect to the daemon " + "at machine <"
	  + machineName + "> and port <" + portManagerPort + ">."
	  + "Please make sure that the machine is reachable "
	  + "and portmanager is running");
    }
    finally {
      try {
	if (!portClient.isClosed())
	  portClient.close();
      }
      catch (IOException e) {

	e.printStackTrace();
      }
    }
    return ports;
  }

  private void machinesSanityCheck() throws Exception {

    for (int i = 0; i < machineList.size(); i++) {

      String host = (String) machineList.get(i);

      try {
	InetAddress add = InetAddress.getByName(host);

      }
      catch (Exception e) {
	throw new MPJRuntimeException(e);
      }

    }

  }

  private void readValuesFromMPJExpressConf() {

    FileInputStream in = null;
    DataInputStream din = null;
    BufferedReader reader = null;
    String line = "";

    try {

      String path = mpjHomeDir + File.separator + RTConstants.MPJEXPRESS_CONF_FILE;
      in = new FileInputStream(path);
      din = new DataInputStream(in);
      reader = new BufferedReader(new InputStreamReader(din));

      while ((line = reader.readLine()) != null) {
	if (line.startsWith(RTConstants.MPJ_DAEMON_PORT_KEY)) {
	  D_SER_PORT = Integer.parseInt(MPJUtil.confValue(line));
	} else if (line.startsWith(RTConstants.MPJ_PORTMANAGER_PORT_KEY)) {
	  portManagerPort = Integer.parseInt(MPJUtil.confValue(line));
	} else if (line.startsWith(RTConstants.MPJ_RUN_LOGLEVEL_KEY)) {
	  logLevel = MPJUtil.confValue(line);
	} else if (line.startsWith(RTConstants.MPJEXPRESS_VERSION_KEY)) {
	  VERSION = MPJUtil.confValue(line);
	}
      }

      in.close();

    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void clientSocketInit() throws Exception {

    for (int i = 0; i < machineList.size(); i++) {
      String daemon = (String) machineList.get(i);
      try {

	if (DEBUG && logger.isDebugEnabled()) {
	  logger.debug("Connecting to " + daemon + "@" + D_SER_PORT);
	}
	try {
	  Socket sockClient = new Socket(daemon, D_SER_PORT);
	  if (sockClient.isConnected())
	    peerSockets.add(sockClient);
	  else {

	    throw new MPJRuntimeException("Cannot connect to the daemon "
		+ "at machine <" + daemon + "> and port <" + D_SER_PORT + ">."
		+ "Please make sure that the machine is reachable "
		+ "and running the daemon in 'sane' state");

	  }
	}
	catch (IOException e3) {

	  throw new MPJRuntimeException("Cannot connect to the daemon "
	      + "at machine <" + daemon + "> and port <" + D_SER_PORT + ">."
	      + "Please make sure that the machine is reachable "
	      + "and running the daemon in 'sane' state");
	}

      }
      catch (Exception ccn1) {
	System.out.println(" rest of the exceptions ");
	throw ccn1;
      }
    }

  }

  private void writeFile(String configurationFileData) {
    // Method to write CONF_FILE in user directory that will be later used by
    // MPJ Express Debugger
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(
	  System.getProperty("user.home") + File.separator + RTConstants.MPJDEV_CONF_FILE));
      out.write(configurationFileData);
      out.close();
    }
    catch (IOException e) {

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

}
