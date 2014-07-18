#!/bin/bash
allArguments="$@"  

#First check if its native?
IS_NATIVE="false"
for i in $@; do 
  if [ "$i" == "native" ]; then
			IS_NATIVE="true"
	fi
done

if [ "$IS_NATIVE" == "false" ]; then
	# use javampjdev
	java -jar $MPJ_HOME/lib/starter.jar "$@"	
	
else
   # use natmpjdev
   
version=`grep mpjexpress.version $MPJ_HOME/conf/mpjexpress.conf |cut -d = -f2`
echo "MPJ Express ($version) is started in cluster configuration with native device"  
MACHINESFILE=""
CP=$MPJ_HOME/lib/mpj.jar                                                                              
for i in $@; do     
#echo "i = $i"                                                              
  case $i in                                                                            
    -np)                                                                                    
      # assign number of processes np value to variable NP
			
			shift;                                                  
      NP="$1"                                                                   
      #echo "-np = $NP"
      shift;      	                                               
      ;;                                                                                  
    -dev)                                                                                    
      # assign device dev value to variable DEV 
			shift;                                                       
      DEV="$1"                                                                      
      #echo "-dev = $DEV"  
			shift;                                                         
      ;;
    -machinesfile)                                                                                    
      # assign machinesfile value to variable MACHINESFILE
		  shift;                                                  
      MACHINESFILE="$1"                                                                      
      #echo "-machinesfile = $MACHINESFILE"   
			shift;                                                        
      ;;
    -wdir)                                                                                    
      # assign working directory path value to variable WDIR  
			shift;                                                
      WDIR="$1"                                                                    
      #echo "-wdir = $WDIR"  
			shift;                                                         
      ;;
		-cp)                                                                                    
      # assign classpath value to variable CP  
			shift;                                                
      CP=$CP:"$1"                                                                    
      #echo "-wdir = $WDIR"  
			shift;                                                         
      ;;
	  -Djava.library.path=*)
		 # assign JVM argument -Djava.library.path value to variable DJAVA_LIBRARY_PATH  
		oldIFS=$IFS
		export IFS="="
		line="$1"
		for path in $line; do
		#	echo "$path"
			DJAVA_LIBRARY_PATH="$path"
		done    
	  IFS=$oldIF
		#DJAVA_LIBRARY_PATH="$path"
		#echo "-Djava.library.path = $DJAVA_LIBRARY_PATH"  
		shift;  
    ;;
                                                                                  
  esac                                                                                   
done                                                                                      


CLASS_NAME=$1
shift;
APP_ARGUMENTS=$@

DJAVA_LIBRARY_PATH=$DJAVA_LIBRARY_PATH:"$MPJ_HOME/lib"

MPIRUN_ARGS=" -np $NP";
if [ "$MACHINESFILE" == "" ]; then
	  MPIRUN_ARGS="$MPIRUN_ARGS";
else
		MPIRUN_ARGS="$MPIRUN_ARGS -machinefile $MACHINESFILE";
fi

COMMAND_TO_RUN="mpirun $MPIRUN_ARGS java -cp $CP:. -Djava.library.path=$DJAVA_LIBRARY_PATH $CLASS_NAME 0 0 $DEV $APP_ARGUMENTS"
#run the command
$COMMAND_TO_RUN	  

fi

