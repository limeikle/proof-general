/*
 *  $RCSfile: ReflectionHelper.java,v $
 *
 *  Created on 02 Nov 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ReflectionHelper
 */
public class ReflectionHelper {

	/**
	 * returns a list of Class objects in the specified package which implement
	 * the specified interface; class-loader magic may mean some
	 * packages/classes are missed
	 */
	public static List<Class> findImplementations(String packageName, Class<?> interfaceClass) {
		ArrayList<Class> list = new ArrayList<Class>();
		String p = packageName;
		p = p.replace('.', '/');

		URL pu = ReflectionHelper.class.getResource(p); // eclipse class loader
														// buggers some of this
		if (pu == null && !p.startsWith("/")) {
			p = "/" + p;
			pu = ReflectionHelper.class.getResource(p);
		}
		// Package pp = Package.getPackage(p);
		if (pu == null) {
			return list; // couldn't find package
		}

		File pd = new File(pu.getFile()); // only works for jars ?
		if (pd.exists()) {
			String[] fs = pd.list();
			for (int i = 0; i < fs.length; i++) {
				String cn = fs[i];
				if (cn.endsWith(".class")) {
					cn = cn.substring(0, cn.length() - 6);
				}
				try {
					Class<?> cc = Class.forName(packageName + "." + cn);
					if (!cc.isInterface() && interfaceClass.isAssignableFrom(cc)) {
						list.add(cc);
					}
				} catch (Throwable t) {}
			}
		}
		return list;
	}

	/**
	 * returns a list of Class objects which implement the specified interface;
	 * class-loader magic may mean some packages/classes are missed
	 */
	public static List<Class> findImplementations(Class interfaceClass) {
		Package[] packages = Package.getPackages();
		List<Class> l = new ArrayList<Class>();
		for (int i = 0; i < packages.length; i++) {
			l.addAll(findImplementations(packages[i].getName(),interfaceClass));
		}
		return l;
	}

	/**
	 * Prints the relevant class, method and line number of the caller of this method.
	 */
	public static void printCaller(String text, int levels) {
		StackTraceElement[] el;
		try {
			throw new Exception("printCaller() artificial stack trace");
		} catch (Exception caught) {
			System.err.println(caught.getMessage()+":\n\t"+text);
			el = caught.getStackTrace();
			levels = Math.min((el.length-1), levels);
		}

		for (int i = 1; i <= levels; i++) {
			String ind = "\t("+(i==1?"in":"from")+" ";
			System.err.println(ind+el[i].getClassName()+"."+el[i].getMethodName()+":"+el[i].getLineNumber()+")");
		}
	}

}
