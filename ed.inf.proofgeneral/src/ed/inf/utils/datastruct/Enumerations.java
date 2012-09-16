/*
 *  $RCSfile: Enumerations.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Contains enumeration and iteration methods and classes.
 */
public class Enumerations {

	public static class IteratorEnumeration<Type> implements Enumeration<Type> {
	    Iterator<Type> i;
	    public IteratorEnumeration(Iterator<Type> i) { this.i = i; }
	    public boolean hasMoreElements() { return i.hasNext(); }
	    public Type nextElement() { return i.next(); }
	  }

	public static class EnumerationIterator<Type> implements Iterator<Type> {
	    Enumeration<Type> e;
	    public EnumerationIterator(Enumeration<Type> e) { this.e = e; }
	    public boolean hasNext() { return e.hasMoreElements(); }
	    public Type next() { return e.nextElement(); }
	    public void remove() { throw new UnsupportedOperationException(); }
	  }

	public static class ReverseVectorIterator<Type> implements Iterator<Type> {
	    Vector<Type> v;
	    int i=0;
	    public ReverseVectorIterator(Vector<Type> v) {
	      this.v = v;
	      i = v.size();
	    }
	    public boolean hasNext() {
	      return (i>0);
	    }
	    public Type next() {
	      if (i==0) {
	    	  throw new NoSuchElementException();
	      }
	      return v.elementAt(--i);
	    }
	    public void remove() {
	      throw new UnsupportedOperationException("ReverseVectorIterator doesn't support remove");
	    }
	  }

	public static <Type> Enumeration<Type> makeReverseEnumeration(Enumeration<Type> e) {
	    Vector<Type> v = new Vector<Type>();
	    while (e.hasMoreElements()) {
	      v.add(0, e.nextElement());
	    }
	    return v.elements();
	  }

	public static <Type> Iterator<Type> makeReverseIterator(Iterator<Type> i) {
	    Vector<Type> v = new Vector<Type>();
	    while (i.hasNext()) {
	      v.add(0, i.next());
	    }
	    return v.iterator();
	  }

	public static <Type> Iterator<Type> makeReverseIterator(Vector<Type> v) {
	    return new ReverseVectorIterator<Type>(v);
	  }

	/**
	   * Creates a reverse enumeration from a vector
	   */
	  public static <Type> Enumeration<Type> makeReverseEnumeration(Vector<Type> v) {
	    if (v==null) {
	    	return null;
	    }
	    class ReverseVectorEnumeration<T> implements Enumeration<T> {
	      int count = 0;
	      Vector<T> vr = null;

	      public ReverseVectorEnumeration(Vector<T> v2) { vr = v2; }

	      public boolean hasMoreElements() {
	        return count < vr.size();
	      }

	      public T nextElement() {
	        synchronized (vr) {
	          if (count < vr.size()) {
	            return vr.elementAt((vr.size()-(++count)));
	          }
	        }

	        throw new java.util.NoSuchElementException("Vector Enumeration");
	      }
	    }
	    return new ReverseVectorEnumeration<Type>(v);
	  }


}
