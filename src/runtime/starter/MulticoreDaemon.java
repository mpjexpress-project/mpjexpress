/*
 The MIT License

 Copyright (c) 2005 
   1. Distributed Systems Group, University of Portsmouth
   2. Community Grids Laboratory, Indiana University 

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
 * File         : MulticoreDaemon.java 
 * Author       : Aamir Shafi
 * Created      : Wed Nov 18 13:35:21 PKT 2009
 * Revision     : $Revision: 1.0 $
 * Updated      : $Date: $
 */

package runtime.starter ;

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
import runtime.daemon.*;
import java.util.concurrent.Semaphore ; 

public class MulticoreDaemon {

  private BufferedReader reader = null;
  private InputStream outp = null;
  private Thread[] workers = null;
  private String hostName = null; 
  private PrintStream out = null;
  private Semaphore outputHandlerSem = new Semaphore(1,true); 
  
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

  public MulticoreDaemon (String mcClassName, String mcJarName,
                          int classOrJar, int numOfProcessors, 
                          String workingDirectory,
                          ArrayList<String> jvmArgs,
                          ArrayList<String> appArgs) throws Exception { 

    this.jvmArgs = jvmArgs ; 
    this.appArgs = appArgs ; 
    this.className = mcClassName ; 
    this.processes = numOfProcessors ; 
    this.deviceName = "smpdev" ;
    this.loader = "useLocalLoader" ; //don't need this
    this.URL = workingDirectory ; 
    this.mpjURL = "someDummympjURL" ; 

    /* means we want to run the class file */ 
    //if(classOrJar == 1) { 
      startNewProcess(mcClassName, numOfProcessors, workingDirectory, 
                                          mcJarName, classOrJar) ; 
    //}
    //else if(classOrJar == 2) { 
     /// System.out.println("MulticoreDaemon.constructor.Can't run jar file");
     // System.exit(0); 
    //}

  }

  public void startNewProcess(String mcClassName, int numOfProcessors, 
                              String workingDirectory, String jarName,
			                    int classOrJar) throws Exception { 
	  
    numOfProcs = Runtime.getRuntime().availableProcessors();
    InetAddress localaddr = InetAddress.getLocalHost();
    hostName = localaddr.getHostName();
    
    Map<String,String> map = System.getenv() ;
    mpjHomeDir = map.get("MPJ_HOME");
			    
    //createLogger(mpjHomeDir); 

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug("mpjHomeDir "+mpjHomeDir); 
    }


    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug ("MulticoreDaemon is waiting to accept connections ... ");
    }
      
    wdir = System.getProperty("user.dir");
    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug("wdir "+wdir);
    }

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug ("A client has connected");
    }

    workers = new Thread[1];
	
    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug ("the daemon will start <" + processes + "> threads"); 
    }

    String[] jArgs = jvmArgs.toArray(new String[0]);
	
    boolean now = false;
    boolean noSwitch = true ; 

    for(int e=0 ; e<jArgs.length; e++) {
		
      if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
        MPJRun.logger.debug("jArgs["+e+"]="+jArgs[e]);		
      }
	  
      if(now) {
        String cp = jvmArgs.remove(e); 
 
	//cp = "."+File.pathSeparator+""+
	cp = 
	  	      mpjHomeDir+"/lib/smpdev.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/xdev.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/mpjbuf.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/loader2.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/starter.jar"+
                      File.pathSeparator+""+mpjHomeDir+"/lib/mpiExp.jar"+
	  	      File.pathSeparator+cp;

//        if(jarName != null) 
  //        cp = wdir + "/" + jarName + File.pathSeparator + cp ; 	      

        System.out.println("cp = "+cp) ; 
	    
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

      String cp = mpjHomeDir+"/lib/smpdev.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/xdev.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/mpjbuf.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/loader2.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/starter.jar"+
           File.pathSeparator+""+mpjHomeDir+"/lib/mpiExp.jar" ;

      //jvmArgs.add("."+File.pathSeparator+""+
   //   if(jarName != null) 
     //     cp = wdir + "/" + jarName + File.pathSeparator + cp ; 	      
      jvmArgs.add(cp) ; 

      System.out.println("cp = "+cp) ; 
//jvmArgs.add("."+File.pathSeparator+""+
//mpjHomeDir+"/lib/loader2.jar"+
//File.pathSeparator+""+mpjHomeDir+"/lib/mpj.jar"+
//File.pathSeparator+""+mpjHomeDir+"/lib/log4j-1.2.11.jar"+
 //File.pathSeparator+""+mpjHomeDir+"/lib/wrapper.jar"
//);
    }

    jArgs = jvmArgs.toArray(new String[0]);

    for(int e=0 ; e<jArgs.length; e++) {
      if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
        MPJRun.logger.debug("modified: jArgs["+e+"]="+jArgs[e]);		
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
	
    ex[indx] = "runtime.starter.MulticoreStarter" ; indx++ ; 
    ex[indx] = wdir; indx++ ; 
    ex[indx] = Integer.toString(processes); indx++ ; 
    ex[indx] = deviceName; indx++;
    ex[indx] = loader; indx++;
    ex[indx] = mpjURL ; indx++;
	
    if(className != null) {
      ex[indx] = className;   
    }
    else {
      ex[indx] = jarName ; 
    }
	
    for(int i=0 ; i< aArgs.length ; i++) {
      ex[i+8+jArgs.length] = aArgs[i];
    }
		  
    for (int i = 0; i < ex.length; i++) {
      if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
        MPJRun.logger.debug(i+": "+ ex[i]);
      }
    } 
	
    /*... Making the command finishes here ...*/

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug("creating process-builder object ");
    }

    ProcessBuilder pb = new ProcessBuilder(ex);

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug("wdir ="+wdir) ; 
    }
    
    pb.directory(new File(wdir)); 
	//Map<String, String> m = pb.environment(); 
	//for(String str : m.values()) {
        //  if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
        //    MPJRun.logger.debug("str : "+str);		
	//  }
	//}
    pb.redirectErrorStream(true);

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug("starting the MultithreadStarter.");
    }

    Process p = null;

    try {
      p = pb.start();
    }
    catch (Exception e) {
      e.printStackTrace();
      return ; 
    }

    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug ("started the MultithreadStarter.");
    }

    /*
    synchronized (processVector) {
      for (int i = 0; i < processVector.length; i++) {
        processVector[i].destroy();
      }
      kill_signal = false;
    }*/

    workers = null;
    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug ("Stopping the output");
    }


    String line = "";
    InputStream outp = p.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(outp));
       
    if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
      MPJRun.logger.debug( "outputting ...");
    }

    try {
      do {
        if (!line.equals("")) {
          line.trim(); 
 
          synchronized (this) {
            System.out.println(line);
            //if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
            //  MPJRun.logger.debug(line);
	    //}
          } 
        }
      }  while ( (line = reader.readLine()) != null); 
        // && !kill_signal); 
    }
    catch (Exception e) {
      if(MPJRun.DEBUG && MPJRun.logger.isDebugEnabled()) { 
        MPJRun.logger.debug ("outputHandler =>" + e.getMessage());
      }
      e.printStackTrace();
    } 
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

  Runnable outputHandler = new Runnable() {

    public void run() {


    } //end run.
  }; //end outputHandler.

  public static void main(String args[]) {
    try {
      MulticoreDaemon dae = null ; // new MulticoreDaemon(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

}
