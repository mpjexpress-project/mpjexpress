package mpjdev.natmpjdev.util;

import java.nio.ByteBuffer; 
 
public interface Allocator { 
  ByteBuffer allocate(int size); 
  void free(ByteBuffer b); 
  void freeAll();
  void print(); 
}