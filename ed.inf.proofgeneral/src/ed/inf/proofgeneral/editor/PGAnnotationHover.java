/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Dec 25, 2006 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.projection.AnnotationBag;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import ed.inf.proofgeneral.document.ProofScriptMarkers;

/**
 * The annotation hover for Proof Scripts.  This is based heavily on
 * {@link org.eclipse.jface.text.source.DefaultAnnotationHover}, with a small
 * modification to return the TOOLTIP attribute of annotations generated from
 * markers.  Unfortunately, long messages with new lines don't seem to work
 * well in annotation hovers which get their newlines collapsed somewhere
 * else.  So maybe this isn't worth it and we should drop the extra
 * TOOLTIP thing (simplify our marker methods too).  But what if long
 * error messages are needed?  Unfortunately the Eclipse developers
 * don't seem to work on this, see bug #7257
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=7257 ("helpwanted").
 * NB: general HTML markup doesn't seem to work for hover, although
 * the bold tag <b> </b> is interpreted.
 */
public class PGAnnotationHover extends DefaultAnnotationHover {

	private static String getLongMessage(Annotation annotation) {
		if (annotation instanceof SimpleMarkerAnnotation) {
		    IMarker marker = ((SimpleMarkerAnnotation) annotation).getMarker();
			return marker.getAttribute(ProofScriptMarkers.TOOLTIP,annotation.getText());
		}
		return annotation.getText();
	}
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationHover#getHoverInfo(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	@SuppressWarnings("unchecked")
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		List javaAnnotations= getAnnotationsForLine(sourceViewer, lineNumber);
		if (javaAnnotations != null) {

			if (javaAnnotations.size() == 1) {

				// optimization
				Annotation annotation= (Annotation) javaAnnotations.get(0);
				String message= getLongMessage(annotation);
				if (message != null && message.trim().length() > 0) {
					return formatSingleMessage(message);
				}

			} else {

				List messages= new ArrayList();

				Iterator e= javaAnnotations.iterator();
				while (e.hasNext()) {
					Annotation annotation= (Annotation) e.next();
					String message= getLongMessage(annotation);
					if (message != null && message.trim().length() > 0) {
						messages.add(message.trim());
					}
				}

				if (messages.size() == 1) {
					return formatSingleMessage((String) messages.get(0));
				}
				if (messages.size() > 1) {
					return formatMultipleMessages(messages);
				}
			}
		}
		return null;
	}

	private boolean isRulerLine(Position position, IDocument document, int line) {
		if (position.getOffset() > -1 && position.getLength() > -1) {
			try {
				return line == document.getLineOfOffset(position.getOffset());
			} catch (BadLocationException x) {
			}
		}
		return false;
	}

	// Copied from DefaultAnnotationHover
	private IAnnotationModel getAnnotationModel(ISourceViewer viewer) {
		if (viewer instanceof ISourceViewerExtension2) {
			ISourceViewerExtension2 extension= (ISourceViewerExtension2) viewer;
			return extension.getVisualAnnotationModel();
		}
		return viewer.getAnnotationModel();
	}

	// Copied from DefaultAnnotationHover
	@SuppressWarnings("unchecked")
	private boolean isDuplicateAnnotation(Map messagesAtPosition, Position position, String message) {
		if (messagesAtPosition.containsKey(position)) {
			Object value= messagesAtPosition.get(position);
			if (message.equals(value)) {
				return true;
			}
			if (value instanceof List) {
				List messages= (List)value;
				if  (messages.contains(message)) {
					return true;
				}
				messages.add(message);
			} else {
				ArrayList messages= new ArrayList();
				messages.add(value);
				messages.add(message);
				messagesAtPosition.put(position, messages);
			}
		} else {
			messagesAtPosition.put(position, message);
		}
		return false;
	}

	// Copied from DefaultAnnotationHover (would be handy if this was protected, not private)
	private boolean includeAnnotation(Annotation annotation, Position position, HashMap messagesAtPosition) {
		if (!isIncluded(annotation)) {
			return false;
		}
		String text= annotation.getText();
		return (text != null && !isDuplicateAnnotation(messagesAtPosition, position, text));
	}

	// Copied from DefaultAnnotationHover (would be handy if this was protected, not private)
	@SuppressWarnings("unchecked")
	private List getAnnotationsForLine(ISourceViewer viewer, int line) {
		IAnnotationModel model= getAnnotationModel(viewer);
		if (model == null) {
			return null;
		}
		IDocument document= viewer.getDocument();
		List javaAnnotations= new ArrayList();
		HashMap messagesAtPosition= new HashMap();
		Iterator iterator= model.getAnnotationIterator();

		while (iterator.hasNext()) {
			Annotation annotation= (Annotation) iterator.next();

			Position position= model.getPosition(annotation);
			if (position == null) {
				continue;
			}
			if (!isRulerLine(position, document, line)) {
				continue;
			}
			if (annotation instanceof AnnotationBag) {
				AnnotationBag bag= (AnnotationBag) annotation;
				Iterator e= bag.iterator();
				while (e.hasNext()) {
					annotation= (Annotation) e.next();
					position= model.getPosition(annotation);
					if (position != null && includeAnnotation(annotation, position, messagesAtPosition)) {
						javaAnnotations.add(annotation);
					}
				}
				continue;
			}

			if (includeAnnotation(annotation, position, messagesAtPosition)) {
				javaAnnotations.add(annotation);
			}
		}

		return javaAnnotations;
	}
}
