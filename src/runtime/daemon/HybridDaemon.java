/*
 The MIT License

 Copyright (c) 2005 - 2013
   1. SEECS National University of Sciences and Technology (NUST), Pakistan
   2. Ansar Javed (2013 - 2013)
   3. Mohsan Jameel (2013 - 2013)
   4. Aamir Shafi (2005 -2013) 
   5. Bryan Carpenter (2005 - 2013)
 
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
 * File         : HybridDaemon.java 
 * Author       : Ansar Javed, Mohsan Jameel, Aamir Shafi, Bibrak Qamar
 * Created      : January 30, 2013 6:00:57 PM 2013
 * Revision     : $
 * Updated      : $
 */




package runtime.daemon ;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.log4j.Logger ;
import org.apache.log4j.PropertyConfigurator ;
import org.apache.log4j.PatternLayout ;
import org.apache.log4j.FileAppender ;
import org.apache.log4j.Level ;
import org.apache.log4j.DailyRollingFileAppender ;
import org.apache.log4j.spi.LoggerRepository ;

import runtime.MPJRuntimeException ;  
import java.util.concurrent.Semaphore ; 
import java.util.regex.* ;



public class HybridDaemon {
  private BufferedReader reader = null;
  private InputStream outp = null;
  private String hostName = null; 
  private PrintStream out = null;
  private Semaphore outputHandlerSem = new Semaphore(1,true); 
  
  private String wdir = null ; 
  private int numOfProcs = 0; 
  private int pos = 0; 
  private String deviceName = null;
  private String className = null ;
  private String mpjHome = null ;
  private ArrayList<String> jvmArgs = new ArrayList<String>();
  private ArrayList<String> appArgs = new ArrayList<String>();
  private int processes = 0;
  private int netID = 0;
  private int nioProcs = 0;
  private String nioConfigFile = null;
  private int SMPProcesses = 0;
  private String cmd = null;
  private Process[] processVector = null;

  private String mpjHomeDir = null ;  
  private String loader = null;
  
	
  public HybridDaemon (String mcClassName, String mcJarName,
                          int classOrJar, int numOfProcesses, 
                          String workingDirectory,
                          ArrayList<String> jvmArgs,
                          ArrayList<String> appArgs,  int netProcCount, 
                          int netStartRank, String fileName) throws Exception { 

    this.jvmArgs = jvmArgs ; 
    this.appArgs = appArgs ; 
		
		if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: In the Constructor of HybridDaemon"); 
      MPJDaemon.logger.debug ("HybridDaemon: appArgs: "+appArgs); 
    }
		
    /* FIXME: It's a dirty hack .. */ 
    if(mcJarName.endsWith(".jar")) 
      this.className = mcJarName ; 
    else 
      this.className = mcClassName; 

    this.processes = numOfProcesses ; 
    this.nioProcs = netProcCount ; 
		this.netID = netStartRank ; 
		this.nioConfigFile = fileName;
    this.deviceName = "hybdev" ;
    this.loader = "useLocalLoader" ; //don't need this

    if(workingDirectory == null) { 
      this.wdir = System.getProperty("user.dir") ; 
    } else { 
      this.wdir = workingDirectory ; 
    }
		
		SMPProcesses = getThreadsPerHost(processes, nioProcs, netID);

    startNewProcess(mcClassName, SMPProcesses, workingDirectory, 
                                             mcJarName, classOrJar) ; 

  }

  public void startNewProcess(String mcClassName, int numOfProcessors, 
                              String workingDirectory, String jarName,
			                    int classOrJar) throws Exception { 
	  
    String cmdClassPath = "EMPTY";

    numOfProcs = Runtime.getRuntime().availableProcessors();
    InetAddress localaddr = InetAddress.getLocalHost();
    hostName = localaddr.getHostName();
    
    Map<String,String> map = System.getenv() ;
    mpjHomeDir = map.get("MPJ_HOME");
			    
    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("mpjHomeDir "+mpjHomeDir); 
      MPJDaemon.logger.debug ("HybridDaemon is waiting to accept connections ... ");
      MPJDaemon.logger.debug("wdir "+wdir);
      MPJDaemon.logger.debug ("A client has connected");
    }

    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug ("HybridDaemon: the daemon will start <" + 
            SMPProcesses + "> threads"); 
    }

    String[] jArgs = jvmArgs.toArray(new String[0]);
	
    boolean now = false;
    boolean noSwitch = true ; 

    for(int e=0 ; e<jArgs.length; e++) {
		
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug("jArgs["+e+"]="+jArgs[e]);		
      }
	  
      if(now) {
        cmdClassPath = jvmArgs.remove(e); 

					if(cmdClassPath.matches("(?i).*mpj.jar.*")) {
						//System.out.println("before <"+cmdClassPath+">");
						//System.out.println("mpj.jar is present ...") ;
						cmdClassPath = cmdClassPath.replaceAll("mpj\\.jar","mpi.jar") ; 
						//cmdClassPath.replaceAll(Pattern.quote("mpj.jar"), 
							//           Matcher.quoteReplacement("mpi.jar")) ;
						//System.out.println("after <"+cmdClassPath+">");
						//System.exit(0) ; 
					}
								//adding hybdev.jar and niodev.jar
					String cp = 
	  	      mpjHomeDir+"/lib/hybdev.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/xdev.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/smpdev.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/niodev.jar"+
											File.pathSeparator+""+mpjHomeDir+"/lib/mpjbuf.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/loader2.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/starter.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/mpiExp.jar" ;

        if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
          MPJDaemon.logger.debug("cp = "+cp) ; 
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

							//adding hybdev.jar and niodev.jar
      String cp = mpjHomeDir+"/lib/hybdev.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/xdev.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/smpdev.jar"+
					 File.pathSeparator+""+mpjHomeDir+"/lib/niodev.jar"+
					 File.pathSeparator+""+mpjHomeDir+"/lib/mpjbuf.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/loader2.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/starter.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/mpiExp.jar" ;

      jvmArgs.add(cp) ; 

      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug("cp = "+cp) ; 
      }
    }

    jArgs = jvmArgs.toArray(new String[0]);

    for(int e=0 ; e<jArgs.length; e++) {
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug("modified: jArgs["+e+"]="+jArgs[e]);		
      }
    }

    int CMD_WORDS = 8 ; 
		int HYB_ARGS = 5;
	
     /* FIX ME: BY AMJAD AZIZ : 
	    When launched in Debug Mode */
    if(MPJDaemon.ADEBUG)
      CMD_WORDS++;

    String[] aArgs = appArgs.toArray(new String[0]);
    String[] ex =
            new String[ (CMD_WORDS+jArgs.length+HYB_ARGS+aArgs.length) ];
   if(MPJDaemon.APROFILE)
   		 	ex[0] = "tau_java";
    		else	
    			ex[0] = "java";
	
    for(int i=0 ; i< jArgs.length ; i++) {
      ex[i+1] = jArgs[i];
    }

    int indx = jArgs.length+1;
    if(MPJDaemon.ADEBUG)
    ex[indx++] = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="+MPJDaemon.startingDebugPort;
    ex[indx] = "runtime.daemon.HybridStarter" ; indx++ ; 
    ex[indx] = wdir; indx++ ; 
    ex[indx] = Integer.toString(SMPProcesses); indx++ ; 
    ex[indx] = deviceName; indx++;
    ex[indx] = loader; indx++;
    ex[indx] = cmdClassPath ; indx++;
	
    if(className != null) {
      ex[indx] = className;   
    }
    else {
      ex[indx] = jarName ; 
    }
		if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: Value of Indx: "+indx+
      " Count of args till now: " +(CMD_WORDS+jArgs.length) );
    }
		indx= CMD_WORDS+jArgs.length;
		// args for hybrid device 
		ex[indx+0] = Integer.toString( processes ) ;
		ex[indx+1] = Integer.toString( nioProcs ) ;
		ex[indx+2] = Integer.toString( netID) ;
		ex[indx+3] = nioConfigFile ;
		ex[indx+4] = "niodev" ;
	
    for(int i=0 ; i< aArgs.length ; i++) {
      ex[i+CMD_WORDS+jArgs.length+HYB_ARGS] = aArgs[i] ;
    }
		
		if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: cmd process-builder, index: value ");
    }
		
    for (int i = 0; i < ex.length; i++) {
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug(i+": "+ ex[i]);
      }
    }
	
    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: creating process-builder object ");
    }

    ProcessBuilder pb = new ProcessBuilder(ex);

    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: wdir ="+wdir) ; 
    }
    
    pb.directory(new File(wdir)); 
    pb.redirectErrorStream(true);

    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug("HybridDaemon: starting the HybridStarter.");
    }

    Process p = null;

    try {
      p = pb.start();
    }
    catch (Exception e) {
      e.printStackTrace();
      return ; 
    }

    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug ("started the HybridStarter.");
    }

    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug ("Stopping the output");
    }

    String line = "";
    InputStream outp = p.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(outp));
       
    if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
      MPJDaemon.logger.debug( "outputting ...");
    }

    try {
      do {
        if (!line.equals("")) {
          line.trim(); 
 
          synchronized (this) {
            if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
              MPJDaemon.logger.debug( "HybridDaemon: in synchronized line triming ");
            }
            System.out.println(line);
          } 
        }
      }  while ( (line = reader.readLine()) != null); 
    }
    catch (Exception e) {
      if(MPJDaemon.DEBUG && MPJDaemon.logger.isDebugEnabled()) { 
        MPJDaemon.logger.debug ("outputHandler =>" + e.getMessage());
      }
      e.printStackTrace();
    } 
		
		try {
      p.destroy();
    }
    catch (Exception e) {
      e.printStackTrace();
      return ; 
    }
  }

	//to get total number of SMP Threads on current host
	public int  getThreadsPerHost (int pro, int boxes, int netRank){
		int proPerHost= pro/boxes;
		int rem= pro%boxes;
		if ((netRank+1)<=rem){
			proPerHost++;
		}
		return proPerHost;
	}
	
	
  public static void main(String args[]) {
    try {
      HybridDaemon dae = null ; 
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
