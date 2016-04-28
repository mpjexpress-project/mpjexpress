package mpjdev.natmpjdev.util;

public class Allocators {
	public static Allocator defaultAllocator;
	public static final int capacity;

	static {
		if(System.getProperty("DefaultAllocatorCapacity") == null) {
			capacity = 8388608;
		} else {
			int a;
			try {
				a = Integer.parseInt(System.getProperty("DefaultAllocatorCapacity"));
			} catch (NumberFormatException e) {
				a = 8388608;
			}
			capacity = a;
		}
	}

	public static Allocator getDefaultAllocator()
	{
		if(defaultAllocator == null) {
			synchronized(Allocators.class) {
				if(defaultAllocator == null) {
					BuddyAllocator d = new BuddyAllocator(capacity);
					d.setThrowExceptionOnAllocationFailure(true);
					defaultAllocator = d;
				}
			}
		}
		return defaultAllocator;
	}

	public static Allocator getNewAllocator()
	{
		BuddyAllocator d = new BuddyAllocator(capacity);
		d.setThrowExceptionOnAllocationFailure(true);
		return d;
	}

	public static Allocator getNewAllocator(int cap)
	{
		BuddyAllocator d = new BuddyAllocator(cap);
		d.setThrowExceptionOnAllocationFailure(true);
		return d;
	}
}