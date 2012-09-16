/*
 *  $RCSfile: LastSaveReferenceProvider.java,v $
 *
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.symbols.quickdiff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

import ed.inf.proofgeneral.document.SymbolisedDocument;
import ed.inf.utils.io.UnicodeInputStream;

/**
 * Default provider for the quickdiff display - the saved document is taken as
 * the reference.
 * 
 * da: Nov 06: this is copied directly from org.eclipse.ui.internal.texteditor.quickdiff.
 * Only change is to replace IDocument and Document with SymbolisedDocument in
 * a couple of crucial places.  This allows overridden set method to be called which
 * converts symbols when the document is read, to repair the problem that
 * symbol conversions are otherwise always flagged as edits!
 * Obviously it would be better if there was a cleaner way to override this
 * code without duplication.
 * 
 * FIXME: this means that *every* document undergoes symbol decoding
 * using SymbolisedDocument.set!  A bit confusing for text files
 * which may now have differences indicated when they are not
 * symbolised.  Instead we should check the filetype
 * below (or in SymbolisedDocument) and call ProofScriptDocument.symbolise 
 * before using set here. 
 *
 * @since 3.0
 */
public class LastSaveReferenceProvider implements IQuickDiffReferenceProvider, IElementStateListener {

	/** <code>true</code> if the document has been read. */
	private boolean fDocumentRead= false;
	/**
	 * The reference document - might be <code>null</code> even if <code>fDocumentRead</code>
	 * is <code>true</code>.
	 */
	private SymbolisedDocument fReference= null;
	/**
	 * Our unique id that makes us comparable to another instance of the same
	 * provider. See extension point reference.
	 */
	private String fId;
	/** The current document provider. */
	private IDocumentProvider fDocumentProvider;
	/** The current editor input. */
	private IEditorInput fEditorInput;
	/** Private lock no one else will synchronize on. */
	private final Object fLock= new Object();
	/** The document lock for non-IResources. */
	private final Object fDocumentAccessorLock= new Object();
	/** Document lock state, protected by <code>fDocumentAccessorLock</code>. */
	private boolean fDocumentLocked;
	/**
	 * The progress monitor for a currently running <code>getReference</code>
	 * operation, or <code>null</code>.
	 */
	private IProgressMonitor fProgressMonitor;
	/** The text editor we run upon. */
	private ITextEditor fEditor;

	/**
	 * A job to put the reading of file contents into a background.
	 */
	private final class ReadJob extends Job {

		/**
		 * Creates a new instance.
		 */
		public ReadJob() {
			super(QuickDiffMessages.getString("LastSaveReferenceProvider.LastSaveReferenceProvider.readJob.label")); //$NON-NLS-1$
			setSystem(true);
			setPriority(SHORT);
		}

		/**
		 * Calls
		 * {@link LastSaveReferenceProvider#readDocument(IProgressMonitor, boolean)}
		 * and returns {@link Status#OK_STATUS}.
		 *
		 * {@inheritDoc}
		 *
		 * @param monitor {@inheritDoc}
		 * @return {@link Status#OK_STATUS}
		 */
		@Override
        protected IStatus run(IProgressMonitor monitor) {
			readDocument(monitor, false);
			return Status.OK_STATUS;
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider#getReference(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IDocument getReference(IProgressMonitor monitor) {
		if (!fDocumentRead)
			readDocument(monitor, true); // force reading it
		return fReference;
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider#dispose()
	 */
	public void dispose() {
		IProgressMonitor monitor= fProgressMonitor;
		if (monitor != null) {
			monitor.setCanceled(true);
		}

		IDocumentProvider provider= fDocumentProvider;

		synchronized (fLock) {
			if (provider != null)
				provider.removeElementStateListener(this);
			fEditorInput= null;
			fDocumentProvider= null;
			fReference= null;
			fDocumentRead= false;
			fProgressMonitor= null;
			fEditor= null;
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider#getId()
	 */
	public String getId() {
		return fId;
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#setActiveEditor(org.eclipse.ui.texteditor.ITextEditor)
	 */
	public void setActiveEditor(ITextEditor targetEditor) {
		IDocumentProvider provider= null;
		IEditorInput input= null;
		if (targetEditor != null) {
			provider= targetEditor.getDocumentProvider();
			input= targetEditor.getEditorInput();
		}


		// dispose if the editor input or document provider have changed
		// note that they may serve multiple editors
		if (provider != fDocumentProvider || input != fEditorInput) {
			dispose();
			synchronized (fLock) {
				fEditor= targetEditor;
				fDocumentProvider= provider;
				fEditorInput= input;
			}
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#isEnabled()
	 */
	public boolean isEnabled() {
		return fEditorInput != null && fDocumentProvider != null;
	}

	/*
	 * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#setId(java.lang.String)
	 */
	public void setId(String id) {
		fId= id;
	}

	/**
	 * Reads in the saved document into <code>fReference</code>.
	 *
	 * @param monitor a progress monitor, or <code>null</code>
	 * @param force <code>true</code> if the reference document should also
	 *        be read if the current document is <code>null</code>,<code>false</code>
	 *        if it should only be updated if it already existed.
	 */
	private void readDocument(IProgressMonitor monitor, boolean force) {

		// protect against concurrent disposal
		IDocumentProvider prov= fDocumentProvider;
		IEditorInput inp= fEditorInput;
		SymbolisedDocument doc= fReference;
		ITextEditor editor= fEditor;

		if (prov instanceof IStorageDocumentProvider && inp instanceof IStorageEditorInput) {

			IStorageEditorInput input= (IStorageEditorInput) inp;
			IStorageDocumentProvider provider= (IStorageDocumentProvider) prov;

			if (doc == null) {
				if (force || fDocumentRead)
					doc= new SymbolisedDocument();
				else
					return;
			} else {
				doc.syncSymbolSupport();
			}

			IJobManager jobMgr= Job.getJobManager(); // Platform.getJobManager();

			try {
				IStorage storage= input.getStorage();
				// check for null for backward compatibility (we used to check before...)
				if (storage == null)
					return;
				fProgressMonitor= monitor;
				ISchedulingRule rule= getSchedulingRule(storage);

				// this protects others from not being able to delete the file,
				// and protects ourselves from concurrent access to fReference
				// (in the case there already is a valid fReference)

				// one might argue that this rule should already be in the Job
				// description we're running in, however:
				// 1) we don't mind waiting for someone else here
				// 2) we do not take long, or require other locks etc. -> short
				// delay for any other job requiring the lock on file
				try {
					lockDocument(monitor, jobMgr, rule);

					String encoding;
					if (storage instanceof IEncodedStorage)
						encoding= ((IEncodedStorage) storage).getCharset();
					else
						encoding= null;

					LastSaveReferenceProvider.setDocumentContent(doc, storage, encoding, monitor);
				} finally {
					unlockDocument(jobMgr, rule);
					fProgressMonitor= null;
				}

			} catch (CoreException e) {
				return;
			}

			if (monitor != null && monitor.isCanceled())
				return;

			// update state
			synchronized (fLock) {
				if (fDocumentProvider == provider && fEditorInput == input) {
					// only update state if our provider / input pair has not
					// been updated in between (dispose or setActiveEditor)
					fReference= doc;
					fDocumentRead= true;
					addElementStateListener(editor, prov);
				}
			}
		}
	}

	private ISchedulingRule getSchedulingRule(IStorage storage) {
		if (storage instanceof ISchedulingRule)
			return (ISchedulingRule) storage;
		else if (storage != null)
			return (ISchedulingRule) storage.getAdapter(ISchedulingRule.class);
		return null;
	}

	/* utility methods */

	private void lockDocument(IProgressMonitor monitor, IJobManager jobMgr, ISchedulingRule rule) {
		if (rule != null) {
			jobMgr.beginRule(rule, monitor);
		} else synchronized (fDocumentAccessorLock) {
			while (fDocumentLocked) {
				try {
					fDocumentAccessorLock.wait();
				} catch (InterruptedException e) {
					// nobody interrupts us!
					throw new OperationCanceledException();
				}
			}
			fDocumentLocked= true;
		}
	}

	private void unlockDocument(IJobManager jobMgr, ISchedulingRule rule) {
		if (rule != null) {
			jobMgr.endRule(rule);
		} else synchronized (fDocumentAccessorLock) {
			fDocumentLocked= false;
			fDocumentAccessorLock.notifyAll();
		}
	}

	/**
	 * Adds this as element state listener in the UI thread as it can otherwise
	 * conflict with other listener additions, since DocumentProvider is not
	 * thread-safe.
	 *
	 * @param editor the editor to get the display from
	 * @param provider the document provider to register as element state listener
	 */
	private void addElementStateListener(ITextEditor editor, final IDocumentProvider provider) {
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=66686 and
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=56871

		Runnable runnable= new Runnable() {
			public void run() {
				synchronized (fLock) {
					if (fDocumentProvider == provider)
						// addElementStateListener adds at most once - no problem to call repeatedly
						provider.addElementStateListener(LastSaveReferenceProvider.this);
				}
			}
		};

		Display display= null;
		if (editor != null) {
			IWorkbenchPartSite site= editor.getSite();
			if (site != null) {
				IWorkbenchWindow window= site.getWorkbenchWindow();
				if (window != null) {
					Shell shell= window.getShell();
					if (shell != null)
						display= shell.getDisplay();
				}
			}
		}

		if (display != null && !display.isDisposed()) {
			display.asyncExec(runnable);
		} else {
			runnable.run();
		}
	}

	/**
	 * Initialises the given document with the given stream using the given
	 * encoding.
	 *
	 * @param document the document to be initialised
	 * @param storage the storage which delivers the document content
	 * @param encoding the character encoding for reading the given stream
	 * @param monitor a progress monitor for cancellation, or <code>null</code>
	 * @exception CoreException if the given storage can not be accessed or read
	 */
	private static void setDocumentContent(SymbolisedDocument document, IStorage storage, String encoding, IProgressMonitor monitor) throws CoreException {
		Reader in= null;
		InputStream contentStream = new UnicodeInputStream(storage.getContents());
		try {

			final int DEFAULT_FILE_SIZE= 15 * 1024;

			if (encoding == null)
				in= new BufferedReader(new InputStreamReader(contentStream), DEFAULT_FILE_SIZE);
			else
				in= new BufferedReader(new InputStreamReader(contentStream, encoding), DEFAULT_FILE_SIZE);
			StringBuffer buffer= new StringBuffer(DEFAULT_FILE_SIZE);
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				if (monitor != null && monitor.isCanceled())
					return;

				buffer.append(readBuffer, 0, n);
				n= in.read(readBuffer);
			}

		  document.set(buffer.toString());
		  
		} catch (IOException x) {
			throw new CoreException(new Status(IStatus.ERROR, EditorsUI.PLUGIN_ID, IStatus.OK, "Failed to access or read underlying storage", x)); //$NON-NLS-1$
		} finally {
			try {
				if (in != null)
					in.close();
				else
					contentStream.close();
			} catch (IOException x) {
				// ignore
			}
		}
	}

	/* IElementStateListener implementation */

	/*
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementDirtyStateChanged(java.lang.Object, boolean)
	 */
	public void elementDirtyStateChanged(Object element, boolean isDirty) {
		if (!isDirty && element == fEditorInput) {
			// document has been saved or reverted - recreate reference
			new ReadJob().schedule();
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementContentReplaced(java.lang.Object)
	 */
	public void elementContentReplaced(Object element) {
		if (element == fEditorInput) {
			// document has been reverted or replaced
			new ReadJob().schedule();
		}
	}
	
	/**
	 * Does nothing.
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementContentAboutToBeReplaced(java.lang.Object)
	 */
	public void elementContentAboutToBeReplaced(Object element) {	}


	/**
	 * Does nothing.
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementDeleted(java.lang.Object)
	 */
	public void elementDeleted(Object element) { }

	/**
	 * Does nothing.
	 * @see org.eclipse.ui.texteditor.IElementStateListener#elementMoved(java.lang.Object, java.lang.Object)
	 */
	public void elementMoved(Object originalElement, Object movedElement) { }
}
