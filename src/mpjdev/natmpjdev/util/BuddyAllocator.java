package mpjdev.natmpjdev.util;

import java.nio.ByteBuffer;

public class BuddyAllocator implements Allocator {
	private BinaryTree<BuddyNode> tree;
	private int capacity;
	private boolean throwExceptionOnAllocationFailure = false;

	public BuddyAllocator(int capacity) {
		this.capacity = nextPowerOfTwo(capacity);
		BuddyNode node = new BuddyNode();
		node.buffer = ByteBuffer.allocateDirect(this.capacity);
		node.capacity = this.capacity;
		node.free = true;
		node.parent = null;
		tree = new BinaryTree<BuddyNode>(node);
	}

	@Override
	public synchronized ByteBuffer allocate(int size) {
		ByteBuffer a = allocate(nextPowerOfTwo(size), tree);
		if (a == null && throwExceptionOnAllocationFailure) {
			print();
			throw new IllegalArgumentException("Can not allocate a bytebuffer of size " + size + ".");
		}
		if (a != null)
			a.limit(size);
		return a;
	}

	@Override
	public synchronized void free(ByteBuffer b) {
		BinaryTree<BuddyNode> currentTree = find(b, tree);
		if (currentTree == null) {
			System.out.println("Tried to wrongly free " + b);
			return; // nothing to be freed
		}
		recombine(currentTree);
	}

	@Override
	public synchronized void freeAll() {
		tree.value.free = true;
		tree.setLeftChild(null);
		tree.setRightChild(null);
		tree.value.buffer.clear();
	}

	@Override
	public synchronized void print() {
		tree.print();
	}

	public void setThrowExceptionOnAllocationFailure(boolean val) {
		throwExceptionOnAllocationFailure = val;
	}

	private ByteBuffer allocate(int size, BinaryTree<BuddyNode> currentTree) {
		if (currentTree.isLeaf() && currentTree.value.free && size == currentTree.value.capacity) {
			currentTree.value.free = false;
			return currentTree.value.buffer;
		} else if (currentTree.isLeaf() && currentTree.value.free && currentTree.value.capacity >= size * 2) {
			BuddyNode l = new BuddyNode();
			BuddyNode r = new BuddyNode();
			r.capacity = l.capacity = currentTree.value.capacity / 2;
			r.free = l.free = true;
			r.buffer = slice(currentTree.value.buffer, 0, r.capacity);
			l.buffer = slice(currentTree.value.buffer, r.capacity, l.capacity);
			l.parent = r.parent = currentTree;
			currentTree.setRightChild(new BinaryTree<BuddyNode>(r));
			currentTree.setLeftChild(new BinaryTree<BuddyNode>(l));
			return allocate(size, currentTree);
		} else if (!currentTree.isLeaf()) {
			ByteBuffer a = allocate(size, currentTree.getLeftChild());
			if (a != null) {
				return a;
			} else {
				return allocate(size, currentTree.getRightChild());
			}
		}

		return null;
	}

	private void recombine(BinaryTree<BuddyNode> currentTree) {
		BinaryTree<BuddyNode> parent = currentTree.value.parent;
		if (parent == null)
			return;
		BinaryTree<BuddyNode> other = parent.getLeftChild() == currentTree ? parent.getRightChild() : parent.getLeftChild();
		if (other.isLeaf() && other.value.free) {
			currentTree.value.buffer = null;
			other.value.buffer = null;
			parent.setLeftChild(null);
			parent.setRightChild(null);
			recombine(parent);
		} else {
			currentTree.value.free = true;
			currentTree.value.buffer.clear();
		}
	}

	private BinaryTree<BuddyNode> find(ByteBuffer b, BinaryTree<BuddyNode> currentTree) {
		if (currentTree == null || b == null)
			return null;
		if (currentTree.isLeaf() && currentTree.value.buffer == b) {
			return currentTree;
		}
		if (!currentTree.isLeaf()) {
			BinaryTree<BuddyNode> a = find(b, currentTree.getLeftChild());
			if (a == null)
				return find(b, currentTree.getRightChild());
			return a;
		}
		return null;
	}

	private static final int nextPowerOfTwo(int num) {
		return 1 << (32 - Integer.numberOfLeadingZeros(num - 1));
	}

	/**
	 * Creates a new buffer whose content is a shared subsequence of a buffer.
	 * <p>
	 * The content of the new buffer will start at the specified offset.
	 *
	 * @param buf
	 *            buffer
	 * @param offset
	 *            offset
	 * @return the new buffer.
	 */
	private static ByteBuffer slice(ByteBuffer buf, int offset, int limit) {
		ByteBuffer a = ((ByteBuffer) buf.clear().position(offset)).slice();
		a.limit(limit);
		return a;
	}

	private class BuddyNode {
		public int capacity = 0;
		public boolean free = false; // only matters for leaf nodes
		ByteBuffer buffer = null;
		BinaryTree<BuddyNode> parent;

		@Override
		public String toString() {
			return "Cap: " + capacity + (free ? " Free" : " Used");
		}
	}
}
