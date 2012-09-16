/*
 *  $RCSfile: PathUtils.java,v $
 *
 *  Created on 30 Oct 2006 by Graham Dutton
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.file;
import java.io.File;

import ed.inf.proofgeneral.NotNull;

/**
 * Methods which handle the system path.
 * @author Graham Dutton
 */
public class PathUtils {

	/** Cached path entry.  Use {@link #getPath()} to retrieve this value. */
	@NotNull
	private static String[] cached_path = new String[0];
	
	private static final String ps = File.pathSeparator;
	
	// possible variable names; multiple capitalisations for cross-platform support.
	private static final String[] path_variables = new String[]{ "path", "PATH", "Path" };

	/**
	 * Gets (and initialises the system path).  Prints an error
	 * message if the environment variable PATH (or system property
	 * with the same name) cannot be found.
	 */	@NotNull
	public static String[] getPath() {
		if (cached_path.length == 0) {
			for (String s : path_variables) {
				String[] test = splitPath(System.getenv(s));
				if (test != null && test.length > 0) {
					cached_path = test;
					break;
				}
			}
		}
		if (cached_path.length == 0) {
			System.err.println("Error: path is empty!");
		}
		return cached_path;
	}

	/**
	 * Returns true if this file exists as an absolute file path, or as a file
	 * somewhere in the system path.
	 */
	public boolean fileExists(File file) {
		return ( which(file) != null );
	}

	/**
	 * Returns true if this file exists as an absolute file path, or as a file
	 * somewhere in the system path.
	 */
	public static boolean fileExists(String filename) {
		return ( which(filename) != null );
	}

	/**
	 * Behaves as the Unix 'which' command (except doesn't check for executability)
	 * @param filename the file to search for
	 * @return the path of the file found, or null if none.
	 */
	public static String which(String filename) {
		return which (new File(filename));
	}

	/**
	 * Behaves as the Unix 'which' command.
	 * <i><b>Note</b>: the method does not currently test for executability, but this
	 * will be supported when Java 6 is supported and code [*] is changed within
	 * the class</i>.
	 * see java.io.File#canExecute() on Java 6.
	 * @param file the file to search for
	 * @return the path of the file found, or null if none.
	 */
	public static String which(File file) {
		File testFile = file;
		// direct file path specified?
		if (testFile.getPath().contains(File.separator)) {
			// if (testFile.canExecute()) { return testFile.getPath(); }	// TODO: Uncomment when we use Java 1.6
			if (testFile.canRead()) { return testFile.getPath(); }		// [*] Replace this line with the above
			String[] path = testFile.getPath().split(File.separator);
			if (path.length > 0) testFile = new File(path[path.length-1]);
		}
		// search system path.
		for (String s : getPath()) {
			testFile = new File(s, file.getPath());
			// if ( testFile.canExecute() ) { return testFile.getPath();	} // TODO: Uncomment when we use Java 1.6
			if ( testFile.canRead() ) {	return testFile.getPath();	}	// [*] Replace this line with the above
		}
		return null;
	}

	/**
	 * Splits a PATH variable according to the system's path separator.
	 * @param pathstr the path string to split.
	 * @return an array of file paths.
	 */
	public static String[] splitPath(String pathstr) {
		if (pathstr != null) return pathstr.split(ps);
		return new String[0];
	}

	/**
	 * DEBUG: Constructs a new object, for testing.
	 * @param args multiple binaries to check..
	 */
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (String s : args) {
			String path = which(s);
			sb.append (s+":\t"+(path==null?"not found":path)+"\n");
		}
		System.out.println(sb.toString());
	}

}
