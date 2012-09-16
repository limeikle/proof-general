/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 11, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * A non-null annotation.
 */
@Target({METHOD, FIELD})
public @interface NotNull {

}
