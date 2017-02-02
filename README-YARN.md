## MPI on Hadoop YARN
MPJ Express is a Java based open-source MPI. Orignal codebase is maintained on sourcefourge. 
MPJ Express v0.44 introduced a new runtime to bootstrap MPI processes in a Hadoop cluster. More details can 
be found in the [Research Paper](http://www.sciencedirect.com/science/article/pii/S1877050915011874)

This guide will help the users to launch MPJ Express applications in a Hadoop cluster
###Pre requisites 
- Apache Hadoop v2.3.0 and above
- MPJ Express v0.44

###Apache Hadoop Configurations
- [Single Node Configuration](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/SingleCluster.html)
- [Cluster Configuration](http://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/ClusterSetup.html)

###Before you proceed
- Set the env variables e.g $HADOOP_HOME
- Set configuration files in $HADOOP_CONF_DIR 
- ResourceManager , NodeManager, DataNode and NameNode daemons are running

###Installing MPJ Express
- ```git clone https://github.com/hex-dump/MPJ-Express-Hadoop.git```
- Set MPJ_HOME and PATH variables
  * ```export MPJ_HOME=<path-to-mpjexpress>```
  * ```export PATH=$MPJ_HOME/bin:$PATH```
- Create a new directory for MPI programs. We assume you have created a directory named **mpjusr**
- Compile MPJ Express
  * ```cd $MPJ_HOME```
  * ```ant hadoop```

###Compiling a Hello World Application
- cd to **mpjusr** directory
- Write Hello World parallel Java program and save it as HelloWorld.java  
```
  import mpi.*;
  public class HelloWorld {
    public static void main(String args[]) throws Exception {
      MPI.Init(args);
      int me = MPI.COMM_WORLD.Rank();
      int size = MPI.COMM_WORLD.Size();
       System.out.println("Hi from <"+me+">"); 
      MPI.Finalize();
    }
  }
```
- Compile: ```javac -cp .:$MPJ_HOME/lib/mpj.jar HelloWorld.java```
- Create HelloWorld jar file: ```jar cf HelloWorld.jar HelloWorld.class```

###Running MPJ Express programs in Hadoop cluster
Assuming your hadoop cluster is running, run the HelloWorld application:  
```mpjrun.sh -yarn -np 2 -dev niodev -wdir <path-to-mpjusr> -jar <path-to-HelloWorld.jar> HelloWorld```  

Command line argument details  

| CMD           | Optional  |  Description |  Usage  |
|:------------- | :-----| :------------|:--------------|
| -yarn | NO | Specifies the mpjrun module to invoke YARN runtime |-yarn|
| -np   |   NO | number of processes to launch | -np \<number-of-processes\>|
| -dev      |    NO  |MPJ Express device name|-dev niodev|
|-wdir| NO |current working directory|-wdir \<path-to-working-directory\>|
|-jar| NO | jar file containing the MPJExpress program|-jar \<path-to-jar\>|
|-amMem| YES|Application Master container's memory, default 2048mb| -amMem \<amount-of-memory-in-mb\>|
|-amCores| YES|Application Master container's virtual cores, default 1| -amCores \<number-of-cores\> |
| -containerMem| YES|MPJ container's memory, default 1024mb| -containerMem \<amount-of-memory-in-mb\> |
| -containerCores| YES|  MPJ container's virtual cores, default 1| -containerCores \<number-of-core\>|
| -yarnQueue |YES| name of YARN's scheduler queue|-yarnQueue \<queue-name\>
| -appName|YES|application name|-appName \<application-name\>|
| -amPriority|YES|AM container's priority, default 0|-amPriority \<numerical-value\>|
| -mpjContainerPriority|YES|MPJ container's priority, default 0| -mpjContainerPriority \<numerical-value\>|
| -hdfsFolder|Yes|hdfs folder for staging files, default root folder **/**| -hdfsFolder \<hdfs-folder-path\>|
| -debugYarn| YES| print debug statements| -debugYarn|

Sample mpjrun command:
```mpjrun.sh -yarn -np 2 -dev niodev -wdir <path-to-mpjusr> -amMem 512 -amCores 1 -containerMem 512 -containerCores 1 -yarnQueue default -appName MPJYarn -amPriority 1 -mpjContainerPriority 1 -hdfsFolder /mpj-yarn/ -debugYarn -jar <path-to-HelloWorld.jar> HelloWorld```
