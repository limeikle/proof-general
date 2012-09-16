/*
 *  $RCSfile: LimitedStack.java,v $
 *
 *  Created on 02 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;
import java.util.Stack;


/**
 * A stack with a size limit. Pushing new objects in can push old ones out.
 * This size limit will ONLY APPLY IF <i>PUSH</i> is used to add elements
 * @author Daniel Winterstein
 */
public class LimitedStack<Elem> extends Stack<Elem> {

    private int limit;

    public LimitedStack(int limit) {
        super();
        assert limit > 0 : "Stack limits must be positive";
        this.limit = limit;
    }

    public Elem push(Elem arg0) {
        Elem ret = super.push(arg0);
		if (size() > limit) {
			remove(0);
		}
		return ret;
    }

    @SuppressWarnings("unchecked")
	public Object clone() {
    	LimitedStack<Elem> clone = (LimitedStack<Elem>) super.clone();
    	clone.limit = this.limit;
    	return clone;
    }

}
