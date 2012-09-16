/*
 *  $RCSfile: NumericStringUtils.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Various utilities which handle Strings which represent numerical data of some sort.
 */
public class NumericStringUtils {

	/** creates a string with the given length with 0s on the left */
	  public static String toZeroedString(long i, int len) {
	    String s = Long.toString(i);
	    while (s.length()<len) { s = "0"+s; }
	    return s;
	  }

	/**
	   * returns the current time in YYYY-MM-DD HH:MM:SS.mss format
	   */
	  public static String makeDateString() {
	    return new SimpleDateFormat(NumericStringUtils.DATE_FORMAT_PREFERRED).format(new Date());
	  }

	public static final String DATE_FORMAT_PREFERRED = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	   * parses a string such as '20mb, 3kb', returning byte value; returns byte value if no char included;
	   * Understands Teras, Gigs, Megs, Kilos, Bytes (capitalised letters needed)
	   */
	  public static long parseFileSizeString(String t) {
	    if (t==null) {
			return 0;
		}
	    String param = t;
	    while (t.length()>0 && !Character.isLetterOrDigit(t.charAt(0))) {
	    	t = t.substring(1);
	    }
	    if (t.length()==0) {
	    	return 0;
	    }
	    int i=0;
	    while (i<t.length() && (Character.isDigit(t.charAt(i))||t.charAt(i)=='.')) {
			i++;
		}
	    if (i==0) {
			throw new NumberFormatException("'"+param+"' is not a valid size string");
		}
	    float f = Float.parseFloat(t.substring(0, i));
	    t = t.substring(i);
	    while (t.length()>0 && !Character.isLetterOrDigit(t.charAt(0))) {
	    	t = t.substring(1);
	    }
	    i=0;
	    while (i<t.length() && Character.isLetter(t.charAt(i))) {
			i++;
		}
	    String specifier = t.substring(0, i).toLowerCase();
	    if (specifier.length()==0) {
	      if (t.length()==0) {
			return (long)f;  //millis if nothing else
		}
	      throw new NumberFormatException("'"+param+"' is not a valid size string");
	    }
	    long v;
	    if (specifier.startsWith("m")) {
	      //assume hour
	      v = (long)(f*1024*1024);
	    } else if (specifier.startsWith("k")) {
	      v = (long)(f*1024);
	    } else if (specifier.startsWith("g")) {
	      v = (long)(f*1024*1024*1024);
	    } else if (specifier.startsWith("t")) {
	      v = (long)(f*1024*1024*1024*1024);
	    } else {
			throw new NumberFormatException("'"+param+"' is not a valid size string");
		}
	    t = t.substring(i);
	    try {
	      v += NumericStringUtils.parseTimeString(t);
	    } catch (NumberFormatException e) {
	      throw new NumberFormatException("'"+param+"' is not a valid size string");
	    }
	    return v;
	  }

	/**
	 * Formats a double-precision number as a string.
	 * @param d the double to format
	 * @param numDecs the number of decimal places to display.
	 * @return a string representing the number to the specified number of places.
	 */
	public static String doubleWithPrecision(double d, int numDecs) {
		long n = 1;
		for (int i = 0; i < numDecs; i++) {
			d *= 10;
			n *= 10;
		}
		return "" + (((int) d) * 1.0) / n;
	}

	/** given an elapsed time in ms, makes it readable, eg 44d 6h, or 8s 923ms */
	  public static String makeTimeString(long t) {
	    long d = (t/1000/60/60/24);
	    long h = ( (t % (1000*60*60*24))/1000/60/60 );
	    long m = ( (t % (1000*60*60))/1000/60 );
	    long s = ( (t % (1000*60))/1000 );
	    long ms = ( (t % (1000))/1 );
	    String result = "";
	    if (d>0) { result += d+"d "; }
	    if (d>0 || h>0) { result += h+"h "; }
	    if (d==0 && (h>0 || m>0)) { result += m+"m "; }
	    if (d==0 && h==0 && (m>0 || s>0)) { result += s+"s "; }
	    if (d==0 && h==0 && m==0 && s<10) { result += ms+"ms "; }
	    if (result.endsWith(" ")) { result=result.substring(0, result.length()-1); }
	    return result;
	  }

	public static String makeTimeStringNano(long tn) {
	  	long tnm = tn % 1000000;
	  	long t = tn/1000000;
	    String result = "";                          //@maydo, doesn't do rounding; oh now i think it does, but check
	    if (t>=100 || (t>0 && tnm==0)) {
	    	long d = (t/1000/60/60/24);
	    	long h = ( (t % (1000*60*60*24))/1000/60/60 );
	    	long m = ( (t % (1000*60*60))/1000/60 );
	    	long s = ( (t % (1000*60))/1000 );
	    	long ms = ( (t % (1000))/1 );
	    	if (d>0) { result += d+"d "; }
	    	if (d>0 || h>0) { result += h+"h "; }
	    	if (d==0 && (h>0 || m>0)) { result += m+"m "; }
	    	if (d==0 && h==0 && (m>0 || s>0)) {
	    		if (m>0 || s<10) {
					result += s+"s ";
				} else {
					result += (s+"."+(NumericStringUtils.makePaddedString(""+Math.round(ms/10.0d), 2, "0", ""))+"s ");
				}
	    	}
	    	if (d==0 && h==0 && m==0 && s<10) { result += ms+"ms "; }
	  	} else {
	  		if (t>=10) {
	  			result += t+"."+NumericStringUtils.makePaddedString(""+Math.round(tnm/10000.0d),2,"0","");
	  		}
	  		else if (t>0) {
	  			result += t+"."+NumericStringUtils.makePaddedString(""+Math.round(tnm/1000.0d),3,"0","");
	  		}
	  		else if (tnm==0) {
	  			result = "0";
	  		}
	  		else if (tnm%1000==0) {
	  			result += t+"."+NumericStringUtils.makePaddedString(""+Math.round(tnm/1000.0d),3,"0","");  //normally only microsec precision
	  		}
	  		//else result += t+"."+makePaddedString(""+(tnm),6,"0","");  //though maybe more somewhere (not that i've seen from nanoTime, but derived maybe)
	  		if (result.length()>0) {
	  			result+="ms";
	  		}
	  		else {
	  			result = tnm+"ns";
	  		}
	  	}
	    if (result.endsWith(" ")) {
	    	result=result.substring(0, result.length()-1);
	    }
	    return result;
	  }

	/** parses a string such as 2h 30m (or even 2.5h), returning ms value; returns ms value if no char included;
	   *  understnads Hours, Minutes, Seconds, MS/MILLIs, Days, Weeks, MONths (approx), Years (capitalised letters needed) */
	  public static long parseTimeString(String t) {
	    if (t==null) {
			return 0;
		}
	    String param = t;
	    while (t.length()>0 && !Character.isLetterOrDigit(t.charAt(0))) {
	    	t = t.substring(1);
	    }
	    if (t.length()==0) {
	    	return 0;
	    }
	    int i=0;
	    while (i<t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i)=='.')) {
	      i++;
	    }
	    if (i==0) {
	    	throw new NumberFormatException("'"+param+"' is not a valid time string");
	    }
	    float v = Float.parseFloat(t.substring(0, i));
	    t = t.substring(i);
	    while (t.length()>0 && !Character.isLetterOrDigit(t.charAt(0))) {
	    	t = t.substring(1);
	    }
	    i=0;
	    while (i<t.length() && Character.isLetter(t.charAt(i))) {
	      i++;
	    }
	    String specifier = t.substring(0, i).toLowerCase();
	    long result = 0;
	    if (specifier.length()==0) {
	      if (t.length()==0) {
	    	  return (long)v;  //millis if nothing else
	      }
	      throw new NumberFormatException("'"+param+"' is not a valid time string");
	    }
	    if (specifier.startsWith("h")) {
	      //assume hour
	      result = (long)(v*60*60*1000);
	    } else if (specifier.startsWith("s")) {
	      result = (long)(v*1000);
	    } else if (specifier.equalsIgnoreCase("ms") || specifier.startsWith("millis")) {
	      result = (long)v;
	    } else if (specifier.equalsIgnoreCase("mon")) {
	      result = (long)(v*30*24*60*60*1000);
	    } else if (specifier.startsWith("m")) {
	      result = (long)(v*60*1000);
	    } else if (specifier.startsWith("d")) {
	      result = (long)(v*24*60*60*1000);
	    } else if (specifier.startsWith("w")) {
	      result = (long)(v*7*24*60*60*1000);
	    } else if (specifier.startsWith("y")) {
	      result = (long)(v*365*24*60*60*1000);
	    } else {
			throw new NumberFormatException("'"+param+"' is not a valid time string");
		}
	    t = t.substring(i);
	    try {
	      result += parseTimeString(t);
	    } catch (NumberFormatException e) {
	      throw new NumberFormatException("'"+param+"' is not a valid time string");
	    }
	    return result;
	  }

	/** returns true if d1 is after d2+diffMillis*/
	  public static boolean compareTimeDifference(long d1, long d2, long diffMillis) {
	    return (d2+diffMillis) < d1;
	  }

	/** pads the string with 0's at the left up to len; no padding if i longer than len */
	  public static String makeZeroPaddedString(int i, int len) {
	    return NumericStringUtils.makePaddedString(""+i, len, '0');
	  }

	/** pads the string with "pad" at the left up to len; no padding if base longer than len */
	  public static String makePaddedString(String base, int len, char pad) {
	    String s = ""+(base==null ? "" : base);
	    while (s.length()<len) {
			s=pad+s;
		}
	    return s;
	  }

	/** pads the string with "pad" at the left up to len; no padding if base longer than len */
	  public static String makePaddedString(String base, int len, String left_pad, String right_pad) {
	    String s = ""+(base==null ? "" : base);
	    while (s.length()<len) {
			s=left_pad+s+right_pad;
		}
	    return s;
	  }

	public static String makeTimeString(String t){
		  return makeTimeString(new Long(t).longValue());
	  }

	/** returns a string eg 6.2kb  or 184mb */
	  public static String makeFileSizeString(long size) {
	    String result = "";
	    if (size==0) {
			result = "0";
		} else {
	      double s = size;
	      int mantissa = 0;
	      while (s>1024) {
	        s /= 1024;
	        mantissa++;
	      }
	      if (s>10) {
			result = ""+((int)Math.round(s));
		} else {
			result = ""+(((int)Math.round(s*10))/10.0);
		}
	      result += " ";
	      switch (mantissa) {
	        case 0: break;
	        case 1: result+="k"; break;
	        case 2: result+="m"; break;
	        case 3: result+="g"; break;
	        case 4: result+="t"; break;
	        default: result+="E"+(mantissa*3); break;
	      }
	      result += "b";
	    }
	    return result;
	  }

	/**
	   * Compares the difference in dates.
	   * @param d1 the first date
	   * @param d2 the second date (to wich to apply the difference)
	   * @param diffDays a number of days to add
	   * @return true if d1 is after d2 + diffDays
	   */
	  public static boolean compareDateDifference(String d1, String d2, int diffDays) {
	    try {
	      if (d1.equalsIgnoreCase("never") || d2.equalsIgnoreCase("never")) {
			return false;
		}
	      SimpleDateFormat ddf = new SimpleDateFormat(DATE_FORMAT_PREFERRED);
	      Date dd1 = ddf.parse(d1);
	      Date dd2 = ddf.parse(d2);
	      Calendar c1 = new GregorianCalendar();
	      c1.setTime(dd1);
	      Calendar c2 = new GregorianCalendar();
	      c2.setTime(dd2);
	      //Message.message("Comparing "+ddf.format(c1.getTime())+" after "+ddf.format(c2.getTime())+" + "+diffDays+" days");
	      c2.add(Calendar.DATE, diffDays);
	      return (c1.after(c2));  //is d1 after c2+diffdays ?
	    } catch (Exception e) {
	//      Message.logWarning("General: invalid dates to compare, "+d1+" with "+d2+" ("+e+"); returning false");
	    }
	    return false;
	  }

}
