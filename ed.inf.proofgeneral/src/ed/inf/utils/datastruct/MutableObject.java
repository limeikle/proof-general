/*
 *  $RCSfile: MutableObject.java,v $
 *
 *  Created on 31 Oct 2004 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

/**
 * A mutable object which is <b>not</b> thread safe.
 * Calls to accessor methods must be externally synchronized
 * if they can occur in different threads.
 *
 */
public class MutableObject {

	public MutableObject(Object o) {
		set(o);
	}

	Object o;

	/**
	 * Getter, <b>not</b> thread safe.
	 * @return the object
	 */
	public Object get() {
		return o;
	}

	/**
	 * Setter, <b>not</b> thread safe.
	 * @param o
	 */
	public void set(Object o) {
		this.o = o;
	}
}

