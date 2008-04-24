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
 * File         : Wrapper.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.17 $
 * Updated      : $Date: 2005/09/03 12:48:55 $
 */

/**
 * This class will start one thread per JVM.
 */
package runtime.daemon;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.* ;
import java.util.jar.Attributes ; 
import java.util.jar.JarFile ;

public class Wrapper {

  String URL = null;
  String config = null;
  int processes = 0;
  JarClassLoader classLoader = null;
  URLClassLoader urlClassLoader = null ;
  String name = null;
  String className = null;
  Class c = null;
  String deviceName = null;
  String packageName = null;
  String rank = null;
  String mpjHomeDir = null ; 
  String [] nargs = null ; 
  String loader = null ;
  String mpjURL = null;
  String hostName = null;

  public Wrapper() {
  }
 
  /**
   * Executes MPJ program in a new JVM. This method is invoked in main 
   * method of this class, which is started by MPJDaemon. This method 
   * can only start a new JVM but can't start multiple threads in one 
   * JVM.
   * @param args Arguments to this method. args[0] is URL 'String', args[1]
   *             number of processes, args[2] is deviceName, and args[3]
   *             is rank started by this process. args[4] is className ...
   *             it will only be used if URL does not point to a 
   *             JAR file.
   */
  public void execute(String args[]) throws Exception {
    InetAddress localaddr = InetAddress.getLocalHost();
    hostName = localaddr.getHostName();
    URL = args[0];
    processes = (new Integer(args[1])).intValue();
    deviceName = args[2];
    rank = args[3];
    loader = args[4] ; 
    mpjURL = args[5] ;
    className = args[6] ; 
    
    nargs = new String[ (args.length-7) ] ;
    System.arraycopy(args, 7, nargs, 0, nargs.length) ;

    String conf = URL.substring(0, (URL.lastIndexOf("/") + 1));
    config = conf + "mpjdev.conf";

    if(loader.equals("useRemoteLoader")) { 
      if(className.equals("dummy")) {
        try {
         URL[] urls = { new URL(URL), new URL(mpjURL) }; 		
         classLoader = new JarClassLoader(urls);
         name = classLoader.getMainClassName();
         c = classLoader.loadClass(name);
         //packageName = c.getPackage().getName();
         //name = packageName+name;
       }
       catch (Exception e) {
         e.printStackTrace();
       }
     }
     else {
       try {
         urlClassLoader = URLClassLoader.newInstance(new URL[] {
                               new URL(URL), new URL(mpjURL) });
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
        c = Class.forName(name) ;
      }
      else {
        name = className ;
        c = Class.forName(name);
      }
    }

    String[] arvs = new String[(nargs.length+3)];
    arvs[0] = rank ;
    arvs[1] = config ;
    arvs[2] = deviceName ;
    
    for(int i=0 ; i<nargs.length ; i++) {
      arvs[i+3] = nargs[i]; 
    }

    try {

      System.out.println("Starting process <"+rank+"> on <"+hostName+">"); 	    
      if(classLoader != null &&
                   loader.equals("useRemoteLoader") ) {
        classLoader.invokeClass(c, arvs);
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
    catch (Exception e) {
      e.printStackTrace();
    }

  } //end execute

  public static void main(String args[]) throws Exception {
    Wrapper wrap = new Wrapper();
    wrap.execute(args);
    //System.out.println("Last Call");
  } //end main
}
