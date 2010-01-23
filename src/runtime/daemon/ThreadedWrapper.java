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
 * File         : ThreadedWrapper.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.19 $
 * Updated      : $Date: 2006/10/20 17:24:47 $
 */

/**
 * This class 'can' start multiple threads in a JVM ...we are using 
 * it with niodev and one process per node. 
 */
package runtime.daemon;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.* ;
import java.util.jar.Attributes ; 
import java.util.jar.JarFile ; 


public class ThreadedWrapper {

  String URL = null;
  String mpjURL = null ;
  public BufferedReader bufferedReader = null;
  public String config = null;
  int processes = 0;
  JarClassLoader classLoader = null;
  URLClassLoader urlClassLoader = null ; 
  String name = null;
  String className = null ;
  Class c = null;
  String deviceName = null;
  String packageName = null;
  String mpjHomeDir = null ;
  String [] nargs = null ; 
  String loader = null ;
  String hostName = null ;

  public ThreadedWrapper() {
  }

  /**
   * Executes MPJ program in a new JVM. This method is invoked in main 
   * method of this class, which is started by MPJDaemon. This method 
   * can start multiple threads in a JVM. This will also parse
   * configuration file.
   * @param args Arguments to this method. args[0] is URL 'String', args[1]
   *             is number of processes, args[2] is deviceName, 
   *             args[3] is className ..this will be used if the URL 
   *             (first arg) doesn't point to a JAR-file.
   */
  public void execute(String args[]) throws Exception {
    InetAddress localaddr = InetAddress.getLocalHost();
    hostName = localaddr.getHostName();
    URL = args[0];
    processes = (new Integer(args[1])).intValue();
    deviceName = args[2];
    loader = args[3] ; 
    mpjURL = args[4]; 
    className = args[5];

    nargs = new String[ (args.length-6) ] ;
    System.arraycopy(args, 6, nargs, 0, nargs.length) ;

    String conf = URL.substring(0, (URL.lastIndexOf("/") + 1)); 
    config = conf+"mpjdev.conf";
    
    URL aURL = null;
    InputStream in = null;

    if(loader.equals("useRemoteLoader")) { 
      if(className.equals("dummy")) {
        try {
         URL[] urls = { new URL(URL), new URL(mpjURL) }; 		
         classLoader = new JarClassLoader(urls);
         name = classLoader.getMainClassName();
         c = classLoader.loadClass(name);
       }
       catch (Exception e) {
         e.printStackTrace();
       }
     }
     else { 
       try {	    
         //System.out.println(" t2 cls lder "+
	 //		 this.getClass().getClassLoader() ); 	       
	 //System.out.println("mpjURL "+mpjURL); 
         urlClassLoader = URLClassLoader.newInstance(new URL[] { 
	                       new URL(mpjURL), 
   	                       new URL(URL)
	                       } ) ;
         //*hack* to call addURL method, but in the end we decided not to 
	 //use it ...
         //urlClassLoader = (URLClassLoader) this.getClass().getClassLoader() ; 
         //Class sysClass = URLClassLoader.class ;

	 //final Class [] parameters = new Class []{URL.class};
	 //Method method = sysClass.getDeclaredMethod("addURL",parameters);
	 //method.setAccessible(true);
	 //method.invoke(urlClassLoader, new Object[] { new URL( URL)  });
	
	 name = className ;         
	 c = urlClassLoader.loadClass(name);
       }
       catch(Exception e){
         e.printStackTrace() ; 	      
       }
     }
    }
    else {
      if(className.equals("dummy")) {
        String jarFileName = URL.substring(URL.lastIndexOf("/")+1, 
			URL.length()); 
        JarFile jarFile = new JarFile(jarFileName) ;
	Attributes attr = jarFile.getManifest().getMainAttributes(); 
        name = attr.getValue(Attributes.Name.MAIN_CLASS); 
	//this stuff should work without loading it because these 
	//classes should have been loaded already.
	//c = ClassLoader.getSystemClassLoader().loadClass(name); 
	c = Class.forName(name) ; 
      }
      else {
        name = className ;
	c = Class.forName(name); 
      }
    }

    try {
      aURL = new URL(new String(config));
      in = aURL.openStream();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    bufferedReader = new BufferedReader(new InputStreamReader(in));

    boolean loop = true;
    Thread[] workers = new Thread[processes];
    //System.out.println("total-processes<" + processes + ">");
    //if(MPJDaemon.hostNameVector.size() == 0) {  
    //  MPJDaemon.initMatchMeStuff() ;  	     
    //} 	    

    for (int j = 0; j < workers.length; j++) {
      workers[j] = new Thread(worker);
      workers[j].start();
      // this delay is not good!
      //try { Thread.currentThread().sleep(10000); }catch(Exception e){}
    }

    for (int j = 0; j < workers.length; j++) {

      try {
        workers[j].join();
      }
      catch (Exception e) {}
    }

    try {

      bufferedReader.close();
      in.close();
    }
    catch (Exception e) {}
  }

  Runnable worker = new Runnable() {
    public void run() {
      try {
        boolean loop = true;
        String arvs[] = new String[(3+nargs.length)];
        String line = "";
        String rank = null;

        synchronized (bufferedReader) {

          while (loop) {
            line = bufferedReader.readLine();

            if (line != null && MPJDaemon.matchMe(line) ) {

                StringTokenizer tokenizer = new StringTokenizer(line, "@");
                tokenizer.nextToken();
                tokenizer.nextToken();
                rank = tokenizer.nextToken();
                loop = false;

            }

            arvs[0] = rank;
            arvs[1] = config;
            arvs[2] = deviceName;
	    //arvs[3] = mpjHomeDir ; 
            
	    for(int i=0 ; i<nargs.length ; i++) {
              arvs[i+3] = nargs[i];
            }
		
          } //end while

        } //end sync.

        System.out.println("Starting process <"+rank+"> on <"+hostName+">");

	if(ThreadedWrapper.this.classLoader != null && 
			loader.equals("useRemoteLoader") ) { 
          ThreadedWrapper.this.classLoader.invokeClass(
              ThreadedWrapper.this.c, arvs);
	}
	else {
	  Method m = c.getMethod("main", new Class[] {arvs.getClass()});
	  m.setAccessible(true);
	  int mods = m.getModifiers();
	  if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
			  !Modifier.isPublic(mods)) {
		  throw new NoSuchMethodException("main");
	  }
	  m.invoke(null, new Object[] {arvs});
	}

        System.out.println("Stopping process <"+rank+"> on <"+hostName+">");
      }
      catch (Exception ioe) {
        //System.out.println("multi-threaded starter: exception"
        //                   + ioe.getMessage());
        ioe.printStackTrace();
      }
    }

  }; //end Runnable

  public static void main(String args[]) throws Exception {
    ThreadedWrapper wrap = new ThreadedWrapper();
    wrap.execute(args);
    //System.out.println("Last Call");
  } //end main
}
