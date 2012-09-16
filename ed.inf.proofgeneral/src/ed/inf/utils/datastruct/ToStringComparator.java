/*
 *  $RCSfile: ToStringComparator.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.util.Comparator;

/**
 * the ToStringComparator compares two objects by comparing the
 * results of <code>toString</code> operations on them both.
 * @see Comparator
 */
public class ToStringComparator<Type> implements Comparator<Type> {
	public int compare(Object o1, Object o2) {
		if (o1==null) {
			if (o2==null) {
				return 0;
			}
			return 1;
		}
		if (o2==null) {
			return -1;
		}
		return (o1.toString().compareTo(o2.toString()));
	}
}

