/*
 *  $RCSfile: DirectoryUtils.java,v $
 *
 *  Created on Created on 31 Oct 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.file;

import java.io.File;

/**
 * Methods which process directories.
 */
public class DirectoryUtils {
	private static final String fs = File.separator;

	/**
	 * Ensures that a String is terminated with a path-separator character.
	 * NOTE: while it is more elegant to simply
	 * <pre>  return path.replaceAll("([^"+ps+"])$","$1"+ps+"");</pre>
	 * this could cause any number of problems depending on how the regexp
	 * treats the path separator character, so a simple <pre>endsWith</pre>
	 * test is instead performed.
	 */
	public static String terminateDirectory(String path) {
		return path.endsWith(fs) ? path : path + fs;
	}

	/**
	 * If a path is a directory, ensures it is terminated with a file-separator character.
	 * If the path is not a directory, ensure it is <b>not</b> terminated thus.
	 * @param path the path to check and modify
	 * @param assumeDir if the path is not valid, or cannot be determined,
	 * 		  terminatePath assumes it's a directory (if true), or a file (if false)
	 * @return a directory string+"/"
	 */
	public static String terminatePath(String path, boolean assumeDir) {
		File f = new File(path);
		boolean ends = path.endsWith(fs);
		boolean isdir = ( !(f.exists()) && assumeDir ) || f.isDirectory();

		if (ends == isdir) {
			return path;
		}
		return isdir ? (path + fs) : path.substring(0,path.length()-1);
	}

	/**
	 * Sanitises a URL path, returning a well-terminated canonical path.
	 * @param path the path to sanitise.
	 * @return the new path, or null if the operation failed.
	 */
	public static String getSanitisedDir(final String path) {
		try {
			File f = new File(path);
			// make sure it's valid, and is a directory
			if (!f.isDirectory()) {
				return null;
			}
			// now replace symlinks to provide canonical path
			// not sure if this is best but it's pretty standard
			// (most executables rely on canonical location while running, not symlink location;
			// and relative paths get messed up if trying to use .. with links)
			return terminateDirectory(f.getCanonicalPath());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Makes a (confidently) unique temporary file path in the system temp
	 * directory, of the form $TEMP/[prefix][currenttime].[extension]
	 *
	 * The uniqueness is not guaranteed safe from race conditions, but definitely
	 * doesn't exist when the path is generated.
	 *
	 * <b>Please note</b> that this method will block until it finds a filename
	 * which does not exist!
	 * @string prefix the prefix of the file (or null for "pg")
	 * @string extension the file extension (or null for "tmp")
	 * @return a new, unique temporary file path.
	 */
	public static String getNewTempFile(String prefix, String extension) {
		if (prefix == null) {
			prefix = "pg";
		}
		if (extension == null) {
			extension = "tmp";
		}
		String path = System.getProperty("java.io.tmpdir") + fs + prefix;
		String file = null;

		do {
			file = path + System.currentTimeMillis() + "." + extension;
		} while (new File(file).exists());
		return file;
	}

}
