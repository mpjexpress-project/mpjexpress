package xdev.mxdev ;

import xdev.ProcessID ;
import java.util.UUID;

public class MXProcessID extends ProcessID {
  long processHandle ; 
  int id = 0 ;

  MXProcessID() { 
  }

  MXProcessID(UUID uuid) { 
    super(uuid);	  
  }
}
