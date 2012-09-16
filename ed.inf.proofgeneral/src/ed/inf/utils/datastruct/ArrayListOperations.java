/*
 *  $RCSfile: ArrayListOperations.java,v $
 *
 *  Created on 1 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for Arrays and Lists (and possibly ArrayLists)
 */
public class ArrayListOperations {

// da: favour traversing iterator in reverse direction instead of copying & reversing lists! 
//	/**
//	 * Return a reversed copy of the given list.
//	 * @param list
//	 * @return reversed version of list, in  a new copy.
//	 */
//	public static final <Type> List<Type> reverse(List<Type> list) {
//		int s = list.size();
//		List<Type> answer = new ArrayList<Type>(s);
//		for(Type e : list) {
//			answer.add(e);
//		}
//		return answer;
//	}

	/**
	 * Flatten a map which may contain nested maps (using recursion).
	 * Note that types can't be guaranteed in sub-maps, so list is of wildcard type.
	 * @param map the map to flatten.
	 * @return a list of map entries, from the specified map and all sub-maps
	 */
	public static final <K,V> List<Map.Entry<?,?>> flattenMap(Map<K,V> map) {
	    List<Map.Entry<?,?>> list = new ArrayList<Map.Entry<?,?>>();
	    for(Iterator<Map.Entry<K,V>> i = map.entrySet().iterator(); i.hasNext();) {
	    	Map.Entry<K,V> x = i.next();
	        if (x instanceof Map<?,?>) {
	            list.addAll(flattenMap((Map<?,?>)x));
	        } else {
	        	list.add(x);
	        }
	    }
	    return list;
	}

	/**
	 * Check whether an object occurs in an array. Uses equals().
	 * @param obj the object to search for
	 * @param array the array in which to search for the object
	 * @return true if the object is found, otherwise false.
	 */
	public static final boolean member(Object obj, Object[] array) {
		for (int j=0; j<array.length; j++) {
			if (array[j].equals(obj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Given a collection, produce a comma delimited string.
	 * @param list a list of things
	 * @param delimiter the string by which to delimit the string-list
	 * @return a string consisting of each thing's string representation.
	 */
	public static <Type> String list2string(Collection<Type> list, String delimiter) {
	    String s = "";
	    for(Iterator<Type> i = list.iterator(); i.hasNext();) {
	        Type x = i.next();
	        s += x.toString();
	        if (i.hasNext()) {
				s += delimiter;
			}
	    }
	    return s;
	}

	/**
	   * Makes an ArrayList from an array.
	   * @param o the array to convert
	   * @return an ArrayList containing the array's values.
	   */
	  public static <Type> ArrayList<Type> makeArrayList(Type o[]) {
		  ArrayList<Type> a = new ArrayList<Type>();
		  for (Type t : o) a.add(t);
		  return a;
	  }

}
