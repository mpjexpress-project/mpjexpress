# Makefile for the MPJ Express native code. Assumes the following environment 
# variables: MPJ_HOME, MPILIB, JAVA_HOME.

INSTALL_DIR = $(MPJ_HOME)/lib
MPI_INCLUDE_DIR = $(MPILIB)/include

CC = $(MPILIB)/bin/mpicc
CFLAGS = -shared -fPIC
INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(MPI_INCLUDE_DIR)
LLIBS =
LDFLAGS=-Wl,-rpath

JAVAC = $(JAVA_HOME)/bin/javac
JAVA = $(JAVA_HOME)/bin/java

RM = rm
CD = cd
CP = cp
MV = mv
LDCONFIG = ldconfig -n

all : libnativempjdev.so

libnativempjdev.so :
	$(CC) -o libnativempjdev.so $(CFLAGS) -Wl,-soname $(LDFLAGS) $(INCLUDES) -L/usr/lib64 $(LLIBS) mpjdev_natmpjdev_Comm.c mpjdev_natmpjdev_Intercomm.c mpjdev_natmpjdev_NativeRecvRequest.c mpjdev_natmpjdev_Group.c mpjdev_natmpjdev_Intracomm.c mpjdev_natmpjdev_NativeSendRequest.c

install :
	@$(MV) libnativempjdev.so $(INSTALL_DIR)
	@$(LDCONFIG) $(INSTALL_DIR)

clean :
	@$(RM) libnativempjdev.so
