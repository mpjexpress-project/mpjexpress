        QuickStart Guide: Running MPJ Express on UNIX/Linux/Mac platform
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

This document contains steps to help you execute your first MPJ Express program
on UNIX/Linux/Mac platforms. Windows users should consult README-win.txt.

Pre-requisites
==============

1. Java 1.5 (stable) or higher
2. Apache ant 1.6.2 or higher (Optional)
3. Perl (Optional) 

Running MPJ Express Programs in the Multicore Configuration
===========================================================

1. Download MPJ Express and unpack it. 
2. Set MPJ_HOME and PATH environmental variables:
       export MPJ_HOME=/path/to/mpj/
       export PATH=$PATH:$MPJ_HOME/bin 
       (These above two lines can be added to ~/.bashrc)
3. Write your MPJ Express program (HelloWorld.java) and save it. 
4. Compile: javac -cp .:$MPJ_HOME/lib/mpj.jar HelloWorld.java
5. Execute: mpjrun.sh -np 4 HelloWorld

Running MPJ Express Programs in the Cluster Configuration
=========================================================

1. Download MPJ Express and unpack it. 
2. Set MPJ_HOME and PATH environmental variables:
       export MPJ_HOME=/path/to/mpj/
       export PATH=$PATH:$MPJ_HOME/bin 
       (These above two lines can be added to ~/.bashrc)
3. Write your MPJ Express program (HelloWorld.java) and save it. 
4. Write a machines file (name it "machines") stating host names or IP
   addresses of all machines involved in the parallel execution.
5. Start daemons: mpjboot machines
6. Compile: javac -cp .:$MPJ_HOME/lib/mpj.jar HelloWorld.java
7. Execute: mpjrun.sh -np 4 -dev niodev HelloWorld
8. Stop daemons: mpjhalt machines

Additional Documentation
========================

For more details, see $MPJ_HOME/doc/linuxguide.pdf

Contact and Support
===================

In case you run into issues please consult $MPJ_HOME/doc/linuxguide.pdf. If 
your query/problem is still not resolved, contact us by emailing: 

1. MPJ mailing list: http://www.lists.rdg.ac.uk/mailman/listinfo/mpj-user
2. Aamir Shafi (aamir.shafi@seecs.edu.pk)
3. Bryan Carpenter (bryan.carpenter@port.ac.uk)
4. Mark Baker (http://acet.rdg.ac.uk/~mab)
5. Guillermo Lopez Taboada (http://www.des.udc.es/~gltaboada)
