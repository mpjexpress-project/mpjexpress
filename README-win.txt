           QuickStart Guide: Running MPJ Express on Windows Platform 
                  Last Updated: Fri Jan 14 12:11:47 EST 2011
                                Version 0.38

Introduction
============

MPJ Express is a reference implementation of the mpiJava 1.2 API, which
is an MPI-like API for Java defined by the Java Grande forum. 

MPJ Express can be configured in two ways: 

1. Multicore Configuration: This configuration is used by developers who want 
   to execute their parallel Java applications on multicore or shared 
   memory machines (laptops and desktops).

2. Cluster Configuration: This configuration is used by developers who want to 
   execute their parallel Java applications on distributed memory platforms
   including clusters and network of computers. 

Pre-requisites
==============
1. Java 1.5 (stable) or higher
2. Apache ant 1.6.2 or higher (Optional)
3. Perl (Optional) 

Running MPJ Express Programs in the Multicore Configuration
===========================================================

1. Download MPJ Express and unpack it. 
2. Set MPJ_HOME and PATH environmental variables.
    - Windows XP, Vista, or 7 (assuming mpj is in 'c:\mpj')
      Right-click My Computer->Properties->Advanced tab->Environment Variables and export 
      the following system variables (User variables are not enough)
	  Set the value of variable MPJ_HOME as c:\mpj 
	  Append the c:\mpj\bin directory to the PATH variable
    - Cygwin on Windows (assuming mpj is 'c:\mpj')
	  The recommended way to is to set variables as in Windows
	  If you want to set variables in cygwin shell
          export MPJ_HOME="c:\\mpj"
          export PATH=$PATH:"$MPJ_HOME\\bin" 
3. Write your MPJ Express program (HelloWorld.java) and save it. 
4. Compile: javac -cp .;%MPJ_HOME%/lib/mpj.jar HelloWorld.java
5. Execute: mpjrun.bat -np 4 HelloWorld.java

Running MPJ Express Programs in the Cluster Configuration 
=========================================================

1. Download MPJ Express and unpack it. 
2. Set MPJ_HOME and PATH environmental variables.
    - Windows XP, Vista, or 7 (assuming mpj is in 'c:\mpj')
      Right-click My Computer->Properties->Advanced tab->Environment Variables and export 
      the following system variables (User variables are not enough)
	  Set the value of variable MPJ_HOME as c:\mpj 
	  Append the c:\mpj\bin directory to the PATH variable
    - Cygwin on Windows (assuming mpj is 'c:\mpj')
	  The recommended way to is to set variables as in Windows
	  If you want to set variables in cygwin shell
          export MPJ_HOME="c:\\mpj"
          export PATH=$PATH:"$MPJ_HOME\\bin" 
3. Write your MPJ Express program (HelloWorld.java) and save it. 
4. Write a machines file (name it "machines") stating host names or IP addresses of all 
   machines involved in the parallel execution.
5. Start daemons:
     a. Windows XP: Run %MPJ_HOME%/bin/installmpjd-windows.bat (Vista and 7 users 
	  need to right-click this script and "Run as Administrator")
     b. Goto Control-Panel->Administrative Tools->Services-> MPJ Daemon and start the service. 
        It is important to start the daemon as a user process instead of a SYSTEM process. For this, 
	  right-Click MPJ Daemon ->Properties, click "Log On" tab, For the option "Log on as:",
	  select This account and put in the user name and password of this account, and start 
	  the service. 
6. Compile: javac -cp .;%MPJ_HOME%/lib/mpj.jar HelloWorld.java 
7. Execute: mpjrun.bat -np 4 -dev niodev HelloWorld
8. Stop daemons: Go-to Control-Panel->Administrative Tools->Services-> MPJ Daemon 
   and stop the service.

Known Issues
============

Users of Windows Vista and 7 might need to turn off their firewalls for installing
the MPJ Daemon as service and running MPJ Express programs.

Additional Documentation
========================

For more details, see $MPJ_HOME/doc/windowsguide.pdf

Contact and Support
===================

In case you run into issues please consult $MPJ_HOME/doc/windowsguide.pdf. If 
your query/problem is still not resolved, contact us by emailing: 

1. MPJ mailing list: http://www.lists.rdg.ac.uk/mailman/listinfo/mpj-user
2. Aamir Shafi (aamir.shafi@seecs.edu.pk)
3. Bryan Carpenter (bryan.carpenter@port.ac.uk)
4. Mark Baker (http://acet.rdg.ac.uk/~mab)
5. Guillermo Lopez Taboada (http://www.des.udc.es/~gltaboada)
