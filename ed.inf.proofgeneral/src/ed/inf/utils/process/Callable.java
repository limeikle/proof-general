/*
 *  $RCSfile: Callable.java,v $
 *
 *  Created on 07 Mar 2004 by heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

/**
 * @author heneveld
 */
public interface Callable {
	Object run(Object param[]) throws Exception;
}
