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
 * File         : WrapperSimpleApp.java 
 * Author       : Aamir Shafi, Bryan Carpenter
 * Created      : Sun Dec 12 12:22:15 BST 2004
 * Revision     : $Revision: 1.5 $
 * Updated      : $Date: 2005/08/14 15:03:45 $
 */

package runtime.daemon;

//package org.tanukisoftware.wrapper;
import org.tanukisoftware.wrapper.*;

/*
 * Copyright (c) 1999, 2004 Tanuki Software
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of the Java Service Wrapper and associated
 * documentation files (the "Software"), to deal in the Software
 * without  restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sub-license,
 * and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 * Portions of the Software have been derived from source code
 * developed by Silver Egg Technology under the following license:
 *
 * Copyright (c) 2001 Silver Egg Technology
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sub-license, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 */

// $Log: WrapperSimpleApp.java,v $
// Revision 1.5  2005/08/14 15:03:45  shafia
// no major chnanges except a few for,matting stujff ..
//
// Revision 1.4  2005/07/29 14:03:10  shafia
// put in the meta-data info. on all the source-code ..made sure license is
// pasted in each and every file ...
//
// Revision 1.3  2005/06/27 16:17:07  shafia
// committti
//
// Revision 1.2  2005/06/14 09:23:47  shafia
// Just commited the source files using JBilder u
//
// Revision 1.1.1.1  2005/03/19 16:06:39  shafia
// Initial import.
//
// Revision 1.6  2004/02/16 04:37:20  mortenson
// Modify the WrapperSimpleApp and WrapperStartStopApp so that the main method
// of a class is located even if it exists in a parent class rather than the
// class specified.
//
// Revision 1.5  2004/01/16 04:42:00  mortenson
// The license was revised for this version to include a copyright omission.
// This change is to be retroactively applied to all versions of the Java
// Service Wrapper starting with version 3.0.0.
//
// Revision 1.4  2003/06/19 05:45:02  mortenson
// Modified the suggested behavior of the WrapperListener.controlEvent() method.
//
// Revision 1.3  2003/04/03 04:05:23  mortenson
// Fix several typos in the docs.  Thanks to Mike Castle.
//
// Revision 1.2  2003/02/17 09:52:16  mortenson
// Modify the way exceptions thrown by an application's main method are
// presented to the user by the WrapperSimpleApp and WrapperStartStopApp so
// they no longer look like a problem with Wrapper configuration.
//
// Revision 1.1  2003/02/03 06:55:28  mortenson
// License transfer to TanukiSoftware.org
//

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 *
 * @author Leif Mortenson <leif@tanukisoftware.com>
 * @version $Revision: 1.5 $
 */
public class WrapperSimpleApp
    implements WrapperListener, Runnable {
  /**
   * Application's main method
   */
  private Method m_mainMethod;

  /**
   * Command line arguments to be passed on to the application
   */
  private String[] m_appArgs;

  /**
   * Gets set to true when the thread used to launch the application completes.
   */
  private boolean m_mainComplete;

  /**
   * Exit code to be returned if the application fails to start.
   */
  private Integer m_mainExitCode;

  /**
   * Flag used to signify that the start method is done waiting for the application to start.
   */
  private boolean m_waitTimedOut;

  /*---------------------------------------------------------------
   * Constructors
   *-------------------------------------------------------------*/
  /**
   * Creates an instance of a WrapperSimpleApp.
   */
  private WrapperSimpleApp(Method mainMethod) {
    m_mainMethod = mainMethod;
  }

  /*---------------------------------------------------------------
   * Runnable Methods
   *-------------------------------------------------------------*/
  /**
   * Used to launch the application in a separate thread.
   */
  public void run() {
    Throwable t = null;
    try {
      if (WrapperManager.isDebugEnabled()) {
        System.out.println("WrapperSimpleApp: invoking main method");
      }
      m_mainMethod.invoke(null, new Object[] {m_appArgs});
      if (WrapperManager.isDebugEnabled()) {
        System.out.println("WrapperSimpleApp: main method completed");
      }

      synchronized (this) {
        // Let the start() method know that the main method returned, in case it is
        //  still waiting.
        m_mainComplete = true;
        this.notifyAll();
      }

      return;
    }
    catch (IllegalAccessException e) {
      t = e;
    }
    catch (IllegalArgumentException e) {
      t = e;
    }
    catch (InvocationTargetException e) {
      t = e.getTargetException();
      if (t == null) {
        t = e;
      }
    }

    // If we get here, then an error was thrown.  If this happened quickly
    // enough, the start method should be allowed to shut things down.
    System.out.println();
    System.out.println("WrapperSimpleApp: Encountered an error running main: " +
                       t);

    // We should print a stack trace here, because in the case of an
    // InvocationTargetException, the user needs to know what exception
    // their app threw.
    t.printStackTrace();

    synchronized (this) {
      if (m_waitTimedOut) {
        // Shut down here.
        WrapperManager.stop(1);
        return; // Will not get here.
      }
      else {
        // Let start method handle shutdown.
        m_mainComplete = true;
        m_mainExitCode = new Integer(1);
        this.notifyAll();
        return;
      }
    }
  }

  /*---------------------------------------------------------------
   * WrapperListener Methods
   *-------------------------------------------------------------*/
  /**
   * The start method is called when the WrapperManager is signalled by the
   *	native wrapper code that it can start its application.  This
   *	method call is expected to return, so a new thread should be launched
   *	if necessary.
   * If there are any problems, then an Integer should be returned, set to
   *	the desired exit code.  If the application should continue,
   *	return null.
   */
  public Integer start(String[] args) {
    if (WrapperManager.isDebugEnabled()) {
      System.out.println("WrapperSimpleApp: start(args)");
    }

    Thread mainThread = new Thread(this, "WrapperSimpleAppMain");
    synchronized (this) {
      m_appArgs = args;
      mainThread.start();
      // Wait for two seconds to give the application a chance to have failed.
      try {
        this.wait(2000);
      }
      catch (InterruptedException e) {
      }
      m_waitTimedOut = true;

      if (WrapperManager.isDebugEnabled()) {
        System.out.println(
            "WrapperSimpleApp: start(args) end.  Main Completed="
            + m_mainComplete + ", exitCode=" + m_mainExitCode);
      }
      return m_mainExitCode;
    }
  }

  /**
   * Called when the application is shutting down.
   */
  public int stop(int exitCode) {
    if (WrapperManager.isDebugEnabled()) {
      System.out.println("WrapperSimpleApp: stop(" + exitCode + ")");
    }

    // Normally an application will be asked to shutdown here.  Standard Java applications do
    //  not have shutdown hooks, so do nothing here.  It will be as if the user hit CTRL-C to
    //  kill the application.
    return exitCode;
  }

  /**
   * Called whenever the native wrapper code traps a system control signal
   *  against the Java process.  It is up to the callback to take any actions
   *  necessary.  Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
   *    WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
   *    WRAPPER_CTRL_SHUTDOWN_EVENT
   */
  public void controlEvent(int event) {
    if ( (event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT)
        && WrapperManager.isLaunchedAsService()) {
      // Ignore
      if (WrapperManager.isDebugEnabled()) {
        System.out.println("WrapperSimpleApp: controlEvent(" + event +
                           ") Ignored");
      }
    }
    else {
      if (WrapperManager.isDebugEnabled()) {
        System.out.println("WrapperSimpleApp: controlEvent(" + event +
                           ") Stopping");
      }
      WrapperManager.stop(0);
      // Will not get here.
    }
  }

  /*---------------------------------------------------------------
   * Methods
   *-------------------------------------------------------------*/
  /**
   * Displays application usage
   */
  private static void showUsage() {
    System.out.println();
    System.out.println(
        "WrapperSimpleApp Usage:");
    System.out.println(
        "  java org.tanukisoftware.wrapper.WrapperSimpleApp {app_class} [app_parameters]");
    System.out.println();
    System.out.println(
        "Where:");
    System.out.println(
        "  app_class:      The fully qualified class name of the application to run.");
    System.out.println(
        "  app_parameters: The parameters that would normally be passed to the");
    System.out.println(
        "                  application.");
  }

  /*---------------------------------------------------------------
   * Main Method
   *-------------------------------------------------------------*/
  /**
   * Used to Wrapper enable a standard Java application.  This main
   *  expects the first argument to be the class name of the application
   *  to launch.  All remaining arguments will be wrapped into a new
   *  argument list and passed to the main method of the specified
   *  application.
   */
  public static void main(String args[]) {
    // Get the class name of the application
    if (args.length < 1) {
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }

    // Look for the specified class by name
    Class mainClass;
    try {
      mainClass = Class.forName(args[0]);
    }
    catch (ClassNotFoundException e) {
      System.out.println("WrapperSimpleApp: Unable to locate the class " +
                         args[0] + ": "
                         + e);
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }
    catch (LinkageError e) {
      System.out.println("WrapperSimpleApp: Unable to locate the class " +
                         args[0] + ": "
                         + e);
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }

    // Look for the main method
    Method mainMethod;
    try {
      // getDeclaredMethod will return any method named main in the specified class,
      //  while getMethod will only return public methods, but it will search up the
      //  inheritance path.
      mainMethod = mainClass.getMethod("main", new Class[] {String[].class});
    }
    catch (NoSuchMethodException e) {
      System.out.println(
          "WrapperSimpleApp: Unable to locate a public static main method in class "
          + args[0] + ": " + e);
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }
    catch (SecurityException e) {
      System.out.println(
          "WrapperSimpleApp: Unable to locate a public static main method in class "
          + args[0] + ": " + e);
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }

    // Make sure that the method is public and static
    int modifiers = mainMethod.getModifiers();
    if (! (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))) {
      System.out.println("WrapperSimpleApp: The main method in class " + args[0]
                         + " must be declared public and static.");
      showUsage();
      WrapperManager.stop(1);
      return; // Will not get here
    }

    // Build the application args array
    String[] appArgs = new String[args.length - 1];
    System.arraycopy(args, 1, appArgs, 0, appArgs.length);

    // Create the WrapperSimpleApp
    WrapperSimpleApp app = new WrapperSimpleApp(mainMethod);

    // Start the application.  If the JVM was launched from the native
    //  Wrapper then the application will wait for the native Wrapper to
    //  call the application's start method.  Otherwise the start method
    //  will be called immediately.
    WrapperManager.start(app, appArgs);

    // This thread ends, the WrapperManager will start the application after the Wrapper has
    //  been propperly initialized by calling the start method above.
  }
}

