/*
 *  $RCSfile: ExceptionHandling.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A class containing static methods which work with Exceptions.
 */
public class ExceptionHandling {

	/**
	 * Extracts and formats a stack trace from an exception.
	 * @param e the exception to format
	 * @return the formatted stack trace in string form.
	 */
	public static String getStackTraceString(Exception e) {
		//make the body the stack trace
		StackTraceElement[] st = e.getStackTrace();
		StringBuffer sb = new StringBuffer(e.toString());
		sb.append("\n");
		for (int i=0; i<st.length; i++) {
			sb.append("     "+st[i].toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Prints the current object's String representation to System.out
	 * @param obj the object to print.
	 */
	public static final void trace(Object obj) {
		System.out.println(obj.toString());
	}

	public static String trimStackTraceOverlap(Throwable t1, Throwable t2) {
		StringWriter sto = new StringWriter();
		//    if (t1 instanceof PwpException)
		//      ((PwpException)t1).printUnmolestedStackTrace(new PrintWriterOrStream(new PrintWriter(sto)));
		//    else
		t1.printStackTrace(new PrintWriter(sto));
		StringWriter sth = new StringWriter();
		//    if (t2 instanceof PwpException)
		//      ((PwpException)t2).printUnmolestedStackTrace(new PrintWriterOrStream(new PrintWriter(sth)));
		//    else
		t2.printStackTrace(new PrintWriter(sth));
		StringBuffer sbo = sto.getBuffer();
		StringBuffer sbh = sth.getBuffer();
		//        System.out.println("ORIG\n"+sbo+"\n\nHERE\n"+sbh+"\n\n");
		//        System.out.flush();
		int io = sbo.length()-1;
		int ih = sbh.length()-1;
		int ioe = io;
		while (io>=0 && ih>=0 && sbo.charAt(io)==sbh.charAt(ih)) {
			if (sbo.charAt(io)=='\n') {
				ioe=io;
			}
			io--;
			ih--;
		}
		return sbo.substring(0, ioe+1);
	}

}
