/*
 The MIT License

 Copyright (c) 2013 - 2013
   1. High Performance Computing Group, 
   School of Electrical Engineering and Computer Science (SEECS), 
   National University of Sciences and Technology (NUST)
   2. Khurram Shahzad, Mohsan Jameel, Aamir Shafi, Bryan Carpenter (2013 - 2013)
   

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
 * File         : ProcessUtil.java 
 * Author       : Khurram Shahzad, Mohsan Jameel, Aamir Shafi, Bryan Carpenter
 * Created      : January 30, 2013 6:00:57 PM 2013
 * Revision     : $
 * Updated      : $
 */
package runtime.daemonmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DaemonUtil {
  public static ArrayList<String> runProcess(String[] command,
      boolean waitOutput) {
    ArrayList<String> consoleMessages = new ArrayList<String>();
    try {
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      if (waitOutput) {

	InputStreamReader isr = new InputStreamReader(process.getInputStream());
	BufferedReader br = new BufferedReader(isr);
	String line = null;
	while ((line = br.readLine()) != null) {
	  if (line.trim().length() > 0) {
	    consoleMessages.add(line);
	  }
	}
	process.getInputStream().close();
      }
    }
    catch (IOException ex) {
      if (!Thread.currentThread().isInterrupted())
	System.out.print(ex.getMessage());
      System.out.print(ex.getMessage());
    }
    catch (Exception ex) {
      System.out.print(ex.getMessage());
    }
    return consoleMessages;
  }

  public static ArrayList<String> runProcess(String[] command) {
    return runProcess(command, true);
  }

  public static String getMPJProcessID(String host) {
    ProcessBuilder pb = new ProcessBuilder();
    return getMPJProcessID(host, pb);
  }

  public static ArrayList<String> getJavaProcesses(String host) {
    ProcessBuilder pb = new ProcessBuilder();
    return getJavaProcesses(host, pb);
  }

  public static String getMPJProcessID(String host, ProcessBuilder pb) {
    int index = 0;
    int space = 0;
    String pid = "";
    ArrayList<String> consoleMessages = new ArrayList<String>();
    if (System.getProperty("os.name").startsWith("Windows"))
      consoleMessages = getWinJavaProcesses(host, pb);
    else
      consoleMessages = getJavaProcesses(host, pb);
    for (String message : consoleMessages) {
      index = message.indexOf("MPJDaemon");
      if (index > -1) {
	space = message.indexOf(" ");
	pid = message.substring(0, space);
	return pid;
      }
    }
    return pid;
  }

  public static ArrayList<String> getJavaProcesses(String host,
      ProcessBuilder pb) {
    String[] command = { "ssh", host, "nohup", "jps", "-m", };
    ArrayList<String> consoleMessages = runProcess(command);
    return consoleMessages;

  }

  public static ArrayList<String> getWinJavaProcesses(String host,
      ProcessBuilder pb) {
    String[] command = { "jps", "-m", };
    ArrayList<String> consoleMessages = runProcess(command);
    return consoleMessages;

  }

}
