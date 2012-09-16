/*
 *  $RCSfile: FileUtils.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.utils.datastruct.StringManipulation;

/**
 * Some general-purpose File utility methods.
 * @author Alex Heneveld
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class FileUtils {

	/**
	 * Read a text file
	 * Note: converts line-breaks into line-breaks suitable for the current system
	 * @param file
	 * @param charset a {@link java.nio.charset.Charset <code>charset</code>} value
	 * for the encoding to use opening file.
	 * @return a string containing the file's contents.
	 * @throws IOException
	 */
	public static final String file2String(File file,Charset charset) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		InputStreamReader streamreader = new InputStreamReader(stream,charset);
	    BufferedReader reader = new BufferedReader(streamreader);
	    StringBuilder text = new StringBuilder();
		try {
			while(true) {
			    String line = reader.readLine();
			    if (line==null) {
			    	break;
			    }
			    text.append(line);
			    text.append(StringManipulation.LINEEND);
			}
		} catch (IOException e) {
			// end of file;
		}
		try {reader.close();} catch (IOException e2) {
			// do nothing
		}
		return text.toString();
	}


	 /**
	  * Return a File object for the given a path relative to the
	  * plugin root.
	  * @param path the path
	  * @return the URL or null if it cannot be found
	  */
	// ProofGeneralPlugin.getDefault().getBundle()
	 public static URL findURL(Bundle bundle, String path) {
		 IPath ipath = new Path(path);
		 URL urlbundle = FileLocator.find(bundle, ipath, null);
		 if (urlbundle != null) {
			 	try {
			 		URL url = FileLocator.toFileURL(urlbundle);
			 		return url;
			 	} catch (IOException e) { }
		 }
		 return null;
	 }
	 /**
	  * Return a File object for the given a path relative to the
	  * plugin root.
	  * @param path the path
	  * @return the URL or null if it cannot be found
	  * @deprecated prefer method taking bundle
	  */
	 @Deprecated
    public static URL findURL(String path) {
		 IPath ipath = new Path(path);
		 if (ProofGeneralPlugin.getDefault()!=null) {
			 URL urlbundle = FileLocator.find(ProofGeneralPlugin.getDefault().getBundle(), ipath, null);
			 if (urlbundle != null) {
				 try {
					 URL url = FileLocator.toFileURL(urlbundle);
					 return url;
				 } catch (IOException e) { }
			 }
		 }
		 File f = new File(path);
		 if (f.exists()) {			 		
			 try {
	            return f.toURI().toURL();
            } catch (MalformedURLException e) {
            }
		 }
		 return null;
	 }

	 /**
	  * Return a File object for the given a path in the given bundle.
	  * @param bundle
	  * @param path the path
	  * @return the file or null if it cannot be found
	  */
	 // da: null return value so we can test for possible file existence here.
	 // I've simplified these methods now so we have some reasonable recovery
	 // and pass in full file paths rather than searching when we know location.
	 public static File findFile(Bundle bundle, String path) {
		 URL url = findURL(bundle, path);
		 if (url != null && !url.getFile().equals("")) {
			 return new File(url.getFile());
		 }
		return null;
	 }
	 
	 /**
	  * Find a file in the main plugin, given a path relative to the plugin root.
	  * @param path the path
	  * @return the file or null if it cannot be found
	 */
	public static File findGenericFile(String path) {
		 return findFile(ProofGeneralPlugin.getDefault().getBundle(),path); 
	}
	
	public static File findProverFile(String proverName, String path) {
		if (proverName == null) {
			return findGenericFile(path);
		}
		try {
			AbstractUIPlugin prover = ProofGeneralPlugin.getProverPlugin(proverName);
			return findFile(prover.getBundle(),path);
		} catch (ProverRegistryException e) {
			return null;
		}
	}
	
	 /**
	  * Find a file given either an absolute file-system path or a plugin-relative
	  * path.  Used for locating configuration files that users may override
	  * and keep outside the workspace.  (For locations inside the workspace we
	  * should keep workspace-relative paths).
	  * @param proverName name of prover, or null for a file in generic area
	 * @param path non-null
	 * @return the found file, or null.
	 */
	public static File findFileExt(String proverName, String path) {
		 File file = new File(path);
		 return file.exists() ? file : findProverFile(proverName,path);
	 }
}
