/*
 *  $RCSfile: ProofScriptPropertyPage.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Properties page for proof script files
 * 
 * FIXME da: this comes from example template code.  It's incomplete 
 * for us and I'm not sure if it is useful or not.  We need to think
 * what's useful to report/adjust here.
 * 
 * Possible per-script properties might be:
 * 
 *  - associated theorem prover (although may set this per-project, fixed by filename extension)
 *  - whether to associate with PG (allows for clash between extensions across plugins; maybe resolved
 *    anyway with Open With..).
 *  - whether to use symbols or not (may be handy to turn off on some files with problems?)
 *  - whether to allow interface scripting or not (but global option probably suffices)
 *  - processed status in theorem prover: but this isn't a property of the file, rather the
 *    state of the file in the prover [may compare with CVS properties, though] 
 *  
 * It may also be useful (or not!) to have a proof project property page.
 */
public class ProofScriptPropertyPage extends PropertyPage {

	private static final String PROVER_TITLE = "Theorem prover:";
    private static final String PROVER_PROPERTY = "PROVER";

    private static final String DEFAULT_PROVER = "Isabelle";  // FIXME

	private static final int TEXT_FIELD_WIDTH = 50;

	private Text proverText;

	/**
	 * Constructor for ProofScriptPropertyPage.
	 */
	public ProofScriptPropertyPage() {
		super();
	}

	private void addFirstSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(PROVER_TITLE);

		// Path text field
		Text pathValueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		pathValueText.setText(DEFAULT_PROVER);
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Label for prover field
		Label proverLabel = new Label(composite, SWT.NONE);
		proverLabel.setText(PROVER_TITLE);

		// prover text field
		proverText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
		proverText.setLayoutData(gd);

		// Populate prover text field
		try {
			String prover =
				((IResource) getElement()).getPersistentProperty(
					new QualifiedName("", PROVER_PROPERTY));
			proverText.setText((prover != null) ? prover : DEFAULT_PROVER);
		} catch (CoreException e) {
			proverText.setText(DEFAULT_PROVER);
		}
	}

	/**
	 * @see PropertyPage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
		// Populate the prover text field with the default value
		proverText.setText(DEFAULT_PROVER);
	}
	
	public boolean performOk() {
		// store the value in the prover text field
		try {
			((IResource) getElement()).setPersistentProperty(
				new QualifiedName("", PROVER_PROPERTY),
				proverText.getText());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

}