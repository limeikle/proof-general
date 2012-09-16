/*
 *  $RCSfile: MutableInteger.java,v $
 *
 *  Created on 31 Oct 2004 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

/**
 * A Mutable integer, *not* thread safe.
 * Calls to get/set methods here which can occur in
 * different threads must be synchronized externally.
 */
public class MutableInteger {

	public MutableInteger(int i) {
		set(i);
	}

	int i;

	/**
	 * Getter, <b>not</b> thread safe.
	 * @return the integer.
	 */
	public int get() {
		return i;
	}

	/**
	 * Setter, <b>not</b> thread safe.
	 * @param i
	 */
	public void set(int i) {
		this.i = i;
	}

	public void inc() {
		i++;
	}
}
