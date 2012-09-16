/*
 *  $RCSfile: PGPerspective.java,v $
 *
 *  Created on 18 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/**
 * Creates the default Proof General perspective
 *
 * @author Daniel Winterstein
 */
public class PGPerspective implements IPerspectiveFactory {

	public PGPerspective() {
		super();
	}

	 public void createInitialLayout(IPageLayout layout) {
	 	defineLayout(layout);
	 }

	 public void defineLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();

		IFolderLayout left = layout.createFolder("topLeft", IPageLayout.LEFT, 0.25f, editorArea);
		left.addView(PGViews.ID_PROOF_EXPLORER);
		left.addView(IPageLayout.ID_OUTLINE);
		if (ed.inf.proofgeneral.ProofGeneralPlugin.debug(this)) {
			left.addView(PGViews.ID_PROJECT_EXPLORER);
		}
		left.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		left.addPlaceholder(IPageLayout.ID_RES_NAV);

		layout.addPlaceholder(PGViews.ID_PROOF_OBJECTS,	IPageLayout.RIGHT, 0.75f, editorArea);

		IFolderLayout bottomRow = layout.createFolder("bottomRow", IPageLayout.BOTTOM, 0.75f, editorArea);
		bottomRow.addView(PGViews.ID_LATEST_OUTPUT);
		bottomRow.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottomRow.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		bottomRow.addPlaceholder(IPageLayout.ID_TASK_LIST);
		bottomRow.addPlaceholder(IPageLayout.ID_PROGRESS_VIEW);
		
		// FIXME: DEPGRAPH, move to other plugin.
		//IFolderLayout topRight = layout.createFolder("topRight", IPageLayout.RIGHT, 0.25f,
		//		editorArea);
		//topRight.addPlaceholder(PGViews.DEPENDENCY_GRAPH);
	}
}
