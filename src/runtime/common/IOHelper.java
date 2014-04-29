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
 * File         : IOHelper.java 
 * Author       : Khurram Shahzad, Mohsan Jameel, Aamir Shafi, Bryan Carpenter
 * Created      : Oct 28, 2013
 * Revision     : $
 * Updated      : Nov 05, 2013 
 */

package runtime.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class IOHelper {

  public static String getUniqueID() {

    return UUID.randomUUID().toString();
  }

  public static boolean isNullOrEmpty(String s) {

    if (s == null || s.trim().equals(""))
      return true;

    return false;
  }

  public static void CreateDirectory(String folderPath) {
    File folder = new File(folderPath);
    if (!folder.isDirectory() && !folder.exists()) {
      folder.mkdir();
    }
  }

  public static String[] getFileList(String directoryPath) {

    File directory = new File(directoryPath);

    if (directory.exists()) {
      return directory.list();
    }

    return new String[] {};
  }

  public static String getFileName(String filePath) {
    return filePath.substring(filePath.lastIndexOf("/") + 1);
  }

  public static String readCharacterFile(String path) {

    FileInputStream stream = null;
    InputStreamReader streamReader = null;
    BufferedReader bufferedReader = null;
    StringBuilder buffer = new StringBuilder();

    try {

      stream = new FileInputStream(path);
      streamReader = new InputStreamReader(stream, "UTF-8");
      bufferedReader = new BufferedReader(streamReader);

      String line = null;

      while ((line = bufferedReader.readLine()) != null) {

	buffer.append(line);
      }

    }
    catch (Exception exp) {

      exp.printStackTrace();
      return null;

    }
    finally {

      try {

	bufferedReader.close();
	stream.close();

      }
      catch (Exception e) {

	e.printStackTrace();
      }
    }

    return buffer.toString();
  }

  public static boolean deleteFile(String path) {

    try {
      File f = new File(path);
      if (f.exists()) {
	return f.delete();

      } else {
	System.out.println("File cannot be deleted");
      }

    }
    catch (Exception exp) {

      exp.printStackTrace();
    }

    return false;
  }

  public static String getUniqueName() {
    return UUID.randomUUID().toString();

  }

  public static String getFileNameFromFilePath(String path) {
    String fileName = "";
    File f = new File(path);
    fileName = f.getName();
    return fileName;
  }

  public static String getFilePath(String path) {
    String fileName = "";
    File f = new File(path);
    fileName = f.getName();
    if (!(fileName.equals(""))) {
      path = path.substring(0, path.indexOf(fileName));
    }
    path = removeTag(path);
    return path;
  }

  public static String removeTag(String str) {
    if (str.startsWith("/")) {
      str = str.substring(str.indexOf("/") + 1);
    }
    if (str.endsWith("/")) {
      str = str.substring(0, str.lastIndexOf("/"));
    }
    return str;
  }

  public static String getCharacterDataFromElement(Element e) {
    if (e != null) {
      Node child = e.getFirstChild();

      if (child instanceof CharacterData) {
	CharacterData cd = (CharacterData) child;
	return cd.getData();
      }
    }
    return "?";
  }

  public static byte[] ReadBinaryFile(String filename) {
    byte[] buffer = null;
    File a_file = new File(filename);
    try {
      FileInputStream fis = new FileInputStream(filename);
      int length = (int) a_file.length();
      buffer = new byte[length];
      fis.read(buffer);
      fis.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return buffer;
  }

  public static Boolean writeFile(String path, byte[] contents) {
    Boolean success = false;
    try {
      File outputfile = new File(path);
      FileOutputStream fos = new FileOutputStream(outputfile);
      fos.write(contents);
      fos.flush();
      fos.close();

      success = true;
    }
    catch (Exception ex) {
    }
    return success;
  }

  public static boolean writeCharacterFile(String path, String contents) {

    Writer writer = null;
    FileOutputStream fileOutputStream = null;
    boolean bRet = false;
    try {
      // if(!f.exists())
      File f = new File(path);
      if (!f.exists()) {
	f.createNewFile();
      }
      fileOutputStream = new FileOutputStream(path);
      writer = new OutputStreamWriter(fileOutputStream, "UTF-8");
      writer.write(contents);
      bRet = true;

    }
    catch (Exception exp) {

      exp.printStackTrace();

    }
    finally {

      try {

	writer.close();

      }
      catch (Exception e) {

	e.printStackTrace();
      }
    }
    return bRet;

  }

  public static void zipFolder(String srcDir, String zipFile) {

    try {

      FileOutputStream fos = new FileOutputStream(zipFile);

      ZipOutputStream zos = new ZipOutputStream(fos);

      File srcFile = new File(srcDir);

      addDirToArchive(zos, srcFile);

      // close the ZipOutputStream
      zos.close();

    }
    catch (IOException ioe) {
      System.out.println("Error creating zip file: " + ioe);
    }

  }

  private static void addDirToArchive(ZipOutputStream zos, File srcFile) {

    File[] files = srcFile.listFiles();
    System.out.println("Adding directory: " + srcFile.getName());
    for (int i = 0; i < files.length; i++) {

      // if the file is directory, use recursion
      if (files[i].isDirectory()) {
	addDirToArchive(zos, files[i]);
	continue;
      }

      try {

	System.out.println("tAdding file: " + files[i].getName());

	// create byte buffer
	byte[] buffer = new byte[1024];

	FileInputStream fis = new FileInputStream(files[i]);

	zos.putNextEntry(new ZipEntry(files[i].getName()));

	int length;

	while ((length = fis.read(buffer)) > 0) {
	  zos.write(buffer, 0, length);
	}
	zos.closeEntry();
	// close the InputStream
	fis.close();

      }
      catch (IOException ioe) {
	System.out.println("IOException :" + ioe);
      }

    }

  }

  public static void ExtractZip(String srcFileName, String targetFolder) {

    File srcFile = new File(srcFileName);
    File temp = new File(targetFolder);
    temp.mkdir();

    ZipFile zFile = null;

    try {

      zFile = new ZipFile(srcFile);

      Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zFile.entries();

      while (e.hasMoreElements()) {

	ZipEntry entry = e.nextElement();

	File destinationPath = new File(targetFolder, entry.getName());

	destinationPath.getParentFile().mkdirs();

	if (entry.isDirectory()) {
	  continue;
	} else {

	  BufferedInputStream bis = new BufferedInputStream(
	      zFile.getInputStream(entry));

	  int b;
	  byte buffer[] = new byte[1024];

	  FileOutputStream fos = new FileOutputStream(destinationPath);

	  BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);

	  while ((b = bis.read(buffer, 0, 1024)) != -1) {
	    bos.write(buffer, 0, b);
	  }

	  bos.close();
	  bis.close();

	}

      }

    }
    catch (IOException ioe) {
      System.out.println("Unable to open zip file" + ioe);
    }
    finally {
      try {
	if (zFile != null) {
	  zFile.close();
	}
      }
      catch (IOException ioe) {
	System.out.println("Unable to close zip file" + ioe);
      }
    }
  }

}
