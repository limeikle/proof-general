/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 22, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.utils.eclipse;

import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 *
 */
public class ResourceUtils {

	/**
	 * Find a resource corresponding to the given URI.  The URI should be an absolute
	 * path to somewhere within the workspace.  May return an IProject, IFolder or IFile
	 * resource, as appropriate.  If more than one resource is mapped to a given URI,
	 * only the first will be returned.
	 * @param uri the URI to look for, or null
	 * @return a resource, or null if null input or none can be found.
	 */
	public static IResource findResource(URI uri) {
		if (uri != null) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IFile[] files = root.findFilesForLocationURI(uri);
			if (files.length > 0) {
				return files[0];
			}
			IContainer[] containers = root.findContainersForLocationURI(uri);
			if (containers.length > 0) {
				return containers[0];
			}
		}
		return null;
	}
}
