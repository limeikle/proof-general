/*
 *  $RCSfile: LimitedMap.java,v $
 *
 *  Created on 11 Nov 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.util.*;
/**
 * A map with limited capacity. If the capacity is exceeded, then the oldest entries are dumped.
 * Built on top of HashMap and Stack.
 * @author Daniel Winterstein
 */
public class LimitedMap<K,V> extends HashMap<K,V> {

    private Stack<K> keys;
    private int limit;

    /**
     * Creates a new limited map with the specified limit.
     */
    public LimitedMap(int limit) {
        keys = new Stack<K>();
        assert limit > 0 : "Map size limits must be positive";
        this.limit = limit;
    }

    /**
     * Puts a key/value pair into the map.  If capacity has been reached,
     * discards the oldest entry.
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    public V put(K key, V value) {
    	if (!containsKey(key)) {
    	    keys.push(key);
    	    if (keys.size() > limit) {
    	        K deadKey = keys.remove(0);
    	        remove(deadKey);
    	    }
    	}
        return super.put(key, value);
    }

    /**
     * Puts all, obeying the limited size of the Map.
     * Will discard even entries from the specified map, if capacity is reached.
     * @param m Map from which to add key/value pairs
     * @see java.util.HashMap#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> m) {
    	for (K key : m.keySet()) {
            put(key,m.get(key)); // FIXME: WMI efficiency improvements
        }
    }

    /**
     * Gets the maximum size of this map.
     */
    public int getLimit() {
    	return limit;
    }

    @SuppressWarnings("unchecked")
	public Object clone() {
    	LimitedMap<K,V> clone = (LimitedMap<K,V>) super.clone();
        clone.keys = (Stack<K>) keys.clone();
    	clone.limit = this.limit;
    	return clone;
    }
}
