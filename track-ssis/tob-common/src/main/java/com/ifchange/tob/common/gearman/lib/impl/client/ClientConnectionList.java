package com.ifchange.tob.common.gearman.lib.impl.client;

import java.util.ArrayList;
import java.util.List;

class ClientConnectionList <V, K> {
	private final class Node {
		private Node prev = null;
		private Node next = null;

		private K failKey;
		private final V value;

		public Node(final V value) {
			this.value = value;
		}
	}

	private Node head = null;
	private Node tail = null;

	private int size = 0;

	public int size() {
		return size;
	}

	public final synchronized boolean contains(final V value) {
		Node n = this.head;
		while(n!=null) {
			if(n.value.equals(value)) return true;
			n = n.next;
		}
		return false;
	}

	public final synchronized boolean hasFailKeys() {

		Node n = head;
		while(n != null) {
			if(n.failKey!=null) {
				return true;
			} else {
				n=n.next;
			}
		}

		return false;
	}

	public final synchronized void clear() {
		Node current = head;
		while(current!=null) {
			Node n = current;
			current = current.next;

			n.failKey	= null;
			n.next		= null;
			n.prev		= null;
		}

		head = null;
		tail = null;
		this.size = 0;
	}

	public final synchronized boolean add(final V value) {
		final Node node = new Node(value);

		if(this.head==null) {

			assert tail==null;

			this.head = node;
			this.tail = node;

		} else {

			assert tail!=null;
			assert tail.next==null;

			node.prev = tail;
			tail.next = node;
			tail = node;
		}

		size++;
		return true;
	}

	public final synchronized boolean addFirst(final V value) {
		final Node node = new Node(value);

		if(this.head==null) {
			assert tail==null;

			this.head = node;
			this.tail = node;

		} else {
			assert head.prev==null;

			node.next = this.head;
			this.head.prev = node;
			this.head = node;
		}

		size++;
		return true;
	}

	public final synchronized K remove(final V value) {

		for(Node n=this.head; n!=null; n=n.next) {

			if(!n.value.equals(value)) continue;

			if(n.next!=null) {
				n.next.prev = n.prev;
			} else {
				assert n==this.tail;
				this.tail = n.prev;
			}

			if(n.prev!=null) {
				n.prev.next = n.next;

				if(n.failKey!=null)
					n.prev.failKey = n.failKey;

				size--;
				return null;		// removed value, fail key moved back
			} else {
				assert n==this.head;
				this.head = n.next;

				size--;
				return n.failKey;	// removed value, fail key returned
			}
		}

		return null;	// Value not in structure
	}

	public final synchronized V tryFirst(final K failKey) {
		if (this.head==null || this.tail==null) return null;

		this.tail.failKey = failKey;
		return head.value;
	}

	public final synchronized V peek() {
		return this.tail==null? null: this.tail.value;
	}

	public final synchronized K removeFirst() {
		if(this.head==null) return null;
		assert this.head.prev == null;

		final K failKey = head.failKey;

		this.head = this.head.next;
		if(this.head!=null)
			this.head.prev = null;

		size--;
		return failKey;
	}

	public final synchronized void clearFailKeys() {
		for(Node n = this.head; n!=null; n=n.next) {
			n.failKey = null;
		}
	}

	public synchronized List<V>  createList() {
		List<V> value = new ArrayList<V>(this.size);

		Node node = this.head;
		for(;node!=null; node=node.next) {
			value.add(node.value);
		}

		return value;
	}
}
