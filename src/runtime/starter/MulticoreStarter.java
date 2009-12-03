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
 * File         : MulticoreStarter.java 
 * Author       : Aamir Shafi, Bryan Carpenter, Jawad Manzoor
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.17 $
 * Updated      : $Date: 2009/08/03 12:48:55 $
 */
/**
 *  This class is used for SMP based system stater
 */
package runtime.starter;

import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.lang.Integer;
import runtime.daemon.JarClassLoader ; 

public class MulticoreStarter {

    String URL = null;
    String config = null;
    int processes = 0;
    JarClassLoader classLoader = null;
    URLClassLoader urlClassLoader = null;
    String name = null;
    String className = null;
    String deviceName = null;
    String packageName = null;
    String mpjHomeDir = null;
    String[] nargs = null;
    String loader = null;
    String mpjURL = null;
    String hostName = null;
    String[] arvs = null;
    Method[] m;
    Method[] method;
    int x;
    Class[] c;
    int num;
    static final Object monitor = new Object();
    Integer rank = new Integer(-1);
    static String appPath ="" ;

    public MulticoreStarter() {
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
    URL = args[0]; //this contains working directory ...
    processes = (new Integer(args[1])).intValue();
    deviceName = args[2];
    loader = args[3];
    mpjURL = args[4];
    className = args[5];
    nargs = new String[(args.length - 6)];
    System.arraycopy(args, 6, nargs, 0, nargs.length);

    arvs = new String[(nargs.length + 3)];

//#######################################################

    Runnable[] ab = new Runnable[processes];

    c = new Class[processes];
    m = new Method[processes];
    method = new Method[processes];


    for (x = 0; x < processes; x++) {
      //System.out.println("x " + x);
      ab[x] = new Runnable() {

        String argNew[] = new String[arvs.length];

        public void run() {
          ///// placed /////////
          int index = Integer.parseInt(Thread.currentThread().getName());
          String conf = URL.substring(0, (URL.lastIndexOf("/") + 1));
          config = conf + "mpjdev.conf";
          // System.out.println("conf " + config);

          if (loader.equals("useRemoteLoader")) {
            if (className.equals("dummy")) {
              try {
                System.out.println("Hello i am remote");
                URL[] urls = {new URL(URL), new URL(mpjURL)};
                classLoader = new JarClassLoader(urls);
                name = classLoader.getMainClassName();
                c[index] = classLoader.loadClass(name);
                //packageName = c.getPackage().getName();
                //name = packageName+name;
              } catch (Exception e) {
                e.printStackTrace();
              }
            } else {
              try {
                System.out.println("Hello i am else of remote");
                urlClassLoader = URLClassLoader.newInstance(new URL[]{
                new URL(URL), new URL(mpjURL)});
                name = className;
                c[index] = urlClassLoader.loadClass(name);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          } else {
            try {
              if (className.equals("dummy")) {
                System.out.println("Hello i am jar loader");
                String jarFileName = URL.substring(URL.lastIndexOf("/") + 1,
                URL.length());
                JarFile jarFile = new JarFile(jarFileName);
                Attributes attr = jarFile.getManifest().getMainAttributes();
                name = attr.getValue(Attributes.Name.MAIN_CLASS);
                c[index] = Class.forName(name);
              } else {
                //.. what happens in the case of other devices, how do
                //know the value of MPJ_HOME variable ..  
                String mpjHome = System.getenv("MPJ_HOME");

                // FIXME: Aamir commented out the following two lines .. 
                // they are not relevant and I don't see a point why we need 
                // to put these directories on the classpath.

               // File dir = new File(mpjHome + "/mpj-user");
                //visitAllDirs(dir);
    
                String classPath = System.getProperty("java.class.path") ;

                //System.out.println(" classPath = "+classPath) ; 

                classPath = classPath + ":" + mpjHome+ "/lib/smpdev.jar:"
                                            + mpjHome+ "/lib/xdev.jar:"
                                            + mpjHome+ "/lib/mpjbuf.jar:"
                                            + mpjHome+ "/lib/mpiExp.jar:"
                                            + mpjHome+ "/lib/loader2.jar" ; 

                System.setProperty("java.class.path", classPath) ; 

                String libPath = URL+File.pathSeparator+
                    mpjHome+"/lib/mpi.jar"+File.pathSeparator+
                    mpjHome+"/lib/mpjdev.jar" ; 

                 //FIXME: IF we are running a JAR file, then in the 
                 //    in the first line of libPath, we'll put URL+jarName
                 // .... ....
                              
                //   System.out.println("class path "+cp);
                //   System.out.println("App path " + appPath);
                appPath = appPath  + File.pathSeparator+libPath;

                //System.out.println(Thread.currentThread()+
                  //                        "appPath => "+appPath) ; 

                ClassLoader systemLoader = 
                            ClassLoader.getSystemClassLoader() ; 

                //System.out.println("systemLoader => "+systemLoader) ; 

                StringTokenizer tok = new StringTokenizer(appPath,
                                           File.pathSeparator);
                int count = tok.countTokens();
                String[] tokArr = new String[count];
                File[] f = new File[count];
                URL[] urls = new URL[count];

                for (int i = 0; i < count; i++) {
                  tokArr[i] = tok.nextToken();
                  //System.out.println("App path " + tokArr[i]);
                  f[i] = new File(tokArr[i]);
                  //System.out.println("Absolute path " + f[i]);
                  urls[i] = f[i].toURI().toURL();
                }


                URLClassLoader ucl = new URLClassLoader(urls);

                //System.out.println("parentLoader => "+
                  //                  ucl.getParent()) ;
                Thread.currentThread().setContextClassLoader(ucl);

                name = className;
                // System.out.println("num --" + num + " Thread "
                // +Thread.currentThread()+" Time "+System.nanoTime());
                c[index] = Class.forName(name, true, ucl);
                //  c[num] = Class.forName(name);

              }
             } catch (Exception exx) {
               exx.printStackTrace() ; 
             }
           }

           arvs[1] = config;
           arvs[2] = deviceName;

           for (int i = 0; i < nargs.length; i++) {
             arvs[i + 3] = nargs[i];
           }

           try {

             if (classLoader != null && loader.equals("useRemoteLoader")) {
               System.out.println("Remote loader invoking class");
               classLoader.invokeClass(c[num], arvs);
             } else {
               //       System.out.println("getting method " + num+" Thread "+Thread.currentThread()+" Time "+System.nanoTime());
               // System.out.println(" -- getting method "+num);

               m[index] = c[index].getMethod("main", new Class[]{arvs.getClass()});
               m[index].setAccessible(true);
               int mods = m[index].getModifiers();
               if (m[index].getReturnType() != void.class 
                            || !Modifier.isStatic(mods) ||
                               !Modifier.isPublic(mods)) {
                 throw new NoSuchMethodException("main");
               }
               //  m.invoke(null, new Object[] {arvs});
               method[index] = m[index];
             }
           } catch (Exception exp) {
         }
         ////// placed end //////

         synchronized (monitor) {

           int val = rank.intValue();
           val++;
           rank = new Integer(val);
           arvs[0] = rank.toString();
           argNew[0] = rank.toString();
           //     System.out.println("rank " + rank);
         }

         //   argNew[1] = arvs[1];
         //    argNew[2] = arvs[2];
         for (int k = 1; k < arvs.length; k++) {
           argNew[k] = arvs[k];
           //       System.out.println(" arg new " + argNew[k]);
         }

         //FIXME: need an elegant way to fill the index 1 
         // element, the issue is that it's filled earlier
         // and here we are actually re-writing it ..
         // don't like it ..but atleast works now!
         argNew[1] = (new Integer(processes)).toString() ; 

         try {
           // System.out.println(" num " + index);
           method[index].invoke(null, new Object[]{argNew});
         } catch (Exception e) {

           System.out.println(" exception while invoking in " +
                              Thread.currentThread());
           e.printStackTrace();
           // This should not happen, as we have disabled access checks
         }
       }
     };
   }
   //#######################################################
   ///////////////// cutted

   try {


     int nprocs = processes;

     Thread procs[] = new Thread[nprocs];
     //System.out.println("nprocs " + nprocs);
     for (num = 0; num < nprocs; num++) {

       procs[num] = new Thread(ab[num]);
       String name = String.valueOf(num);
       procs[num].setName(name);
       procs[num].start();

       //System.out.println("thread after start" + num+" Thread "+
       //                   Thread.currentThread()+" Time "+System.nanoTime());
       // procs[num].join();
       //Thread.currentThread().sleep(500);
              
     }


     for (int i = 0; i < nprocs; i++) {
       procs[i].join();
     }

    } catch (Exception e) {
     e.printStackTrace();
    }
  } //end execute

    public static void main(String args[]) throws Exception {
        MulticoreStarter mstarter = new MulticoreStarter();
        mstarter.execute(args);
    } //end main

    public static void visitAllDirs(File dir) {
        if (dir.isDirectory()) {
             appPath+=dir+":"  ;

            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                visitAllDirs(new File(dir, children[i]));
            }
        }
    }

}
