/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 5, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.pgip;

import java.net.URI;

import org.dom4j.Element;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Position;

import ed.inf.utils.eclipse.ResourceUtils;


/**
 * The representation of file locations in PGIP messages, from location attributes.
 * See pgip.rnc/location_attrs.
 * @author David Aspinall
 */
public class Location {

	/** The URI of this location, or null if absent. */
	private URI uri;

	/** A string description for this location, or null if absent. */
	private String description;

	/** The line number for this location, or -1 if absent. */
	private  int line = -1;

	/** The column number for this location, or -1 if absent. */
	private int column = -1;

	/** The offset for this location, or -1 if absent. */
	private int offset = -1;

	/** The length for this location, or -1 if absent. */
	private int length = -1;

	/**
	 * @return the column
	 */
	public int getColumn() {
		return column;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @return the line
	 */
	public int getLine() {
		return line;
	}


	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Return a position (offset + length) for this location.
	 * @return the position, if one can be created, else null.
	 */
	public Position getPosition() {
		if (offset != -1 && length != -1) {
			return new Position(offset,length);
		}
		return null;
	}


	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}


	/**
	 * Test whether this location element actually has any information in it.
	 * @return true if the location has at least a URI, a description, or some character
	 * location.
	 */
	public boolean isUseful() {
		return uri != null || description != null || line != -1 || column != -1 || offset != -1;
	}


	/**
	 * Create a location element given a PGIP message which possibly has
	 * location attributes.
	 * @param pgipmsg the PGIP message to parse.  If the argument is
	 * null or has no location attributes, construct an empty location with
	 * no information.
	 */
	public Location(Element pgipmsg) {
		if (pgipmsg != null) {
			String descrAttr = pgipmsg.attributeValue("location_descr");
			String uriAttr = pgipmsg.attributeValue("location_url");
			String lineAttr = pgipmsg.attributeValue("locationline");
			String columnAttr = pgipmsg.attributeValue("locationcolumn");
			String characterAttr = pgipmsg.attributeValue("locationcharacter");
			String lengthAttr = pgipmsg.attributeValue("locationlength");

			this.description = descrAttr;

			// FIXME: better error reporting here, for each item separately.
			// For easy recovery, we should return a location object with as
			// many fields filled as possible.  Can log conversion errors to
			// plugin log.
			try {
				if (uriAttr != null) {
					this.uri = new URI(uriAttr);
				}
				if (lineAttr != null) {
					this.line = Integer.parseInt(lineAttr);
				}
				if (columnAttr != null) {
					this.column = Integer.parseInt(columnAttr);
				}
				if (characterAttr != null) {
					this.offset = Integer.parseInt(lineAttr);
				}
				if (lengthAttr != null) {
					this.length = Integer.parseInt(lengthAttr);
				}
			} catch (Exception e) {
				System.err.println("Error in parsing location attributes in PGIP message.");
				e.printStackTrace();
			}
		}
	}

	public IResource getResource() {
		return ResourceUtils.findResource(this.uri);
	}


	/* probably not needed, but...
	public boolean equals(Object o) {
		if (o instanceof Location) {
			Location other = (Location) o;
			return this.uri!=null ? this.uri.equals(other.uri) : other.uri==null &&
				    this.column == other.column &&
					this.line == other.line &&
					this.offset == other.offset;
		} else {
			return false;
		}
	}*/

}
