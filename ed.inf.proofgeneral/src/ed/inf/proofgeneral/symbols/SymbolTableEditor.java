/*
 *  $RCSfile: SymbolTableEditor.java,v $
 *
 *  Created on 30 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.symbols;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.dom4j.Element;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.io.StringReaderInputStream;

/**
 Most of the code for this is lifted from the SWT address book example.
 */
public class SymbolTableEditor extends EditorPart implements SelectionListener {
	/**
	 * The id for this editor, as defined in plugin.xml->editors
	 */
	public static final String EDITORID = "ed.inf.proofgeneral.symboltableeditor";


	SymbolTable symbolTable = null;
	boolean isDirty = false;

	/** The table we're editing */
	Table table = null;
	/** A larger font for the unicode symbols. */
	Font bigFont = null;

	public SymbolTableEditor() {
		super();
	}


	@Override
    public void doSave(IProgressMonitor progressMonitor) {
	    try {
	        String text = StringManipulation.convertLineBreak("<proofgeneral>");
	        text += StringManipulation.convertLineBreak("<!-- Symbol table file, created by the Proof General Symbol Table Editor -->");
		    TableItem[] items = table.getItems();
		    Symbol sym;
		    for (TableItem t : items) {
		        sym = (Symbol) t.getData();
		        text += StringManipulation.convertLineBreak("   "+sym.toString());
		    }
		    text += StringManipulation.convertLineBreak("</proofgeneral>");
		    if (getEditorInput() instanceof FileEditorInput) {
		        FileEditorInput ei = (FileEditorInput) getEditorInput();
		        ei.getFile().setContents(new StringReaderInputStream(text),true,false,progressMonitor);
		    } else {
		    	throw new ScriptingException("Unable to save - unrecognised editor input type");
		    }
	        setDirty(false);
	    } catch (CoreException e) {
	    	signalSaveError(progressMonitor,e);
	    } catch (IOException e) {
	    	signalSaveError(progressMonitor,e);
	    } catch (ScriptingException e) {
	    	signalSaveError(progressMonitor,e);
	    }
	}
	private void signalSaveError(IProgressMonitor progressMonitor,Exception ex) {
		ErrorUI.getDefault().signalError(ex);
		ex.printStackTrace();
		progressMonitor.setCanceled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	@Override
    public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	/*
	 * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
    public void init(IEditorSite site, IEditorInput input) {
		setSite(site);
		setInput(input);
    	symbolTable = new SymbolTable();
    	if (input instanceof IPathEditorInput) {
			try {
			    IPath path= ((IPathEditorInput)input).getPath();
			    File f = path.toFile();
			    setPartName(f.getName());
			    if (!f.exists()) {
			    	return;
			    }
			    symbolTable.init(path);
			} catch (Exception e) {
			    ErrorUI.getDefault().signalError(e);
			}
	    }
	}

	@Override
    public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean value) {
	    if (isDirty==value) {
	    	return;
	    }
	    isDirty = value;
	    firePropertyChange(PROP_DIRTY);
	}

	private void newEntry() {
		DataEntryDialog dialog = new DataEntryDialog(table.getShell());
		dialog.setLabels(editColumnNames);
		String[] data = dialog.open();
		if (data != null) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(data);
			try {
				tweakSymbol(item);
			} catch (Exception e) {
			    Exception x = new Exception(e.getMessage()+" \nOnly one of these symbols will actually be displayed.");
			    ErrorUI.getDefault().signalWarning(x);
			}
			setDirty(true);
		}
	}

	@Override
    public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * The fields in symbol objects to display     *
	 */
	String[] columnNames = {"Symbol", "Name", "ASCII", "Shortcut","HTML","Family","Hex","Status"};
	/**
	 * The fields in symbol objects that can be edited
	 */
	String[] editColumnNames = {"Family","Name","Ascii","Shortcut","HTML","Hex","Unicode"/*, "Enabled"*/};
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
    public void createPartControl(Composite parent) {
        FormLayout layout = new FormLayout();
	    parent.setLayout(layout);
	    table = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER
	    		| SWT.V_SCROLL);
//        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
//        table.setLayoutData(layoutData);

        FormData data = new FormData();
        data.left = new FormAttachment(0,5);
        data.right = new FormAttachment(100,-5);
        data.top = new FormAttachment(0,5);
        data.bottom = new FormAttachment(100,-60);

        table.setLayoutData (data);
        table.setLinesVisible(true);
	    //table.setFont();
	    Font tableFont = table.getFont();
	    FontData fd = tableFont.getFontData()[0];
	    bigFont = new Font(parent.getDisplay(), fd.getName(), (int)(fd.getHeight() * 1.2), fd.getStyle());
	    // temporarily set the table font to be large, so as to get large rows
//	    table.setFont(bigFont);
	    table.setHeaderVisible(true);
		for(int i = 0; i < columnNames.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(columnNames[i]);
			column.setWidth(100);
//          TODO: sort column
//            column.addListener(SWT.Selection, new Listener() {
//                public void handleEvent(Event e) {
//
//                    sortColumn(column);
//                    TableItem[] items = table.getItems();
//                    Collator collator = Collator.getInstance(Locale.getDefault());
//                    for (int a = 1; a < items.length; a++) {
//                        String value1 = items[a].getText(0);
//                        for (int j = 0; j < a; j++){
//                            String value2 = items[j].getText(0);
//                            if (collator.compare(value1, value2) < 0) {
//                                String[] values = items[a].getText();
//                                items[a].dispose();
//                                TableItem item = new TableItem(table, SWT.NONE, j);
//                                item.setText(values);
//                                items = table.getItems();
//                                break;
//                            }
//                        }
//                    }
//
//                }
//            });
		}
		fillTable(symbolTable.getAllSymbols());
		// actions and menus
	    IActionBars bars = getEditorSite().getActionBars();
        makeActions();
        bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
		table.setMenu(createPopUpMenu());
		table.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetDefaultSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items.length > 0) {
					editEntry(items[0]);
				}
			}
		});
		table.setEnabled(true);


        Button newButton = new Button(parent,SWT.NONE);
        FormData data2 = new FormData();
        data2.left = new FormAttachment(20);
        data2.right = new FormAttachment(40);
        data2.top = new FormAttachment(table,10);
        data2.bottom = new FormAttachment(100,-10);
        newButton.setLayoutData (data2);
        newButton.setText("New Symbol");
        newButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            public void widgetSelected(SelectionEvent e) {
                newEntry();
            }
        });


        Button applyButton = new Button(parent,SWT.NONE);
        data2 = new FormData();
        data2.left = new FormAttachment(60);
        data2.right = new FormAttachment(80);
        data2.top = new FormAttachment(table,10);
        data2.bottom = new FormAttachment(100,-10);
        applyButton.setLayoutData (data2);
		applyButton.setText("Apply");
		applyButton.setToolTipText("apply changes to the current symbol table. Note: does NOT save the changes.");
		applyButton.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            public void widgetSelected(SelectionEvent e) {
                applyChanges();
            }
		});
//        parent.layout();
        //table.setSize(parent.getSize().x, parent.getSize().y - 100);
		IWorkbenchHelpSystem hs = ProofGeneralPlugin.getDefault().getWorkbench().getHelpSystem();
		hs.setHelp(table,"ed.inf.proofgeneral.symboltableeditor");
		hs.setHelp(parent,"ed.inf.proofgeneral.symboltableeditor");

//        final Composite fParent = parent;
//        parent.addControlListener(new ControlListener() {
//           public void controlMoved(ControlEvent e) {
//           }
//           public void controlResized(ControlEvent e) {
//               Point size = fParent.getSize();
//               table.setSize(size.x-50,size.y-100);
//               fParent.layout();
//           }
//        });

        //Bounds(0,0,parent.getBounds().width,parent.getBounds().height-50);
		// set the table font back to normal
		//table.setFont(tableFont); // TODO: This doesn't work at the moment: SWT tables (at least in Windows) are quite buggy.
	}




	/**
	 * Add the symbols to the table.
	 * @param symbols
	 */
	void fillTable(List<Symbol> symbols) {
		// Just in case the symbol table hasn't been fully initialised.
		//prover.getSymbols().unicodeSupportCheck(table.getFont());
		for(Symbol sym : symbols) {
            makeTableItem(new TableItem(table, SWT.DEFAULT), sym);
		}
	}

    /**
     * Adjust a table item object to properly reflect a symbol.
     * @param item
     * @param sym
     */
    void makeTableItem(TableItem item, Symbol sym) {
        String ucode = "";
        if (sym.unicode != null) {
            for(int j=0; j<sym.unicode.length(); j++) {
            	if (j >0) {
					ucode += ",";
            	}
                String uc = Integer.toHexString(sym.unicode.charAt(j));
                // leading zeros are lost in this, but needed for reconversion
                // so we add them back in:
                if (uc.length()<4) {
                    uc = "0000".substring(uc.length()) + uc;
                }
                ucode += uc;
            }
        }
        if (!sym.status.equals(Symbol.HIDDEN)) {
        	sym.status = Symbol.ENABLED;
        } else {
            sym.status = Symbol.DISABLED;
        }
    	// {"Symbol", "Name", "ASCII", "Shortcut","HTML","Family","Unicode (hex)","Status"};
        item.setFont(0, bigFont);
        Image image = new Image(null, 1, bigFont.getFontData()[0].getHeight() ); // TODO get font height in pixels?
        item.setImage(image);
        item.setData(sym);
        String[] symData = {sym.unicode, sym.name, sym.ascii, sym.shortcut, sym.html,
        		sym.family, ucode, sym.status};
        item.setText(symData);
    }

	/**
	 * Adjust the current symbol table.
	 */
    // TODO: use this to "engage" a particular symbol table.
    // TODO: use a background operation here (workbench progress site).
	void applyChanges() {
	    symbolTable.removeAll();
	    TableItem[] items = table.getItems();
	    Symbol sym;
	    for (int i=0; i<items.length; i++) {
	        sym = (Symbol) items[i].getData();
	        try {
		        Element cmd = DocElement.xml2element(sym.toString());
	            symbolTable.processSymbolTableCommand(cmd);
	        } catch (ScriptingException se) {
	        	System.err.println(se.getLocalizedMessage());
	        	EclipseMethods.errorDialog(se);
	        } catch (Exception x) {
	        	x.printStackTrace();
	        }
	    }
	    // FIXME: ISABELLEWS engage with symbol table for some prover (refresh somehow?)
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
    public void setFocus() {
		// TODO Auto-generated method stub

	}


    public void widgetDefaultSelected(SelectionEvent e) {
        // TODO Auto-generated method stub

    }
    public void widgetSelected(SelectionEvent e) {
        // TODO Auto-generated method stub

    }
	/**
	 * Creates all items located in the popup menu and associates
	 * all the menu items with their appropriate functions.
	 *
	 * @return	Menu
	 *			The created popup menu.
	 */
	private Menu createPopUpMenu() {
		Menu popUpMenu = new Menu(table);
		/**
		 * Adds a listener to handle enabling and disabling
		 * some items in the Edit submenu.
		 */
		popUpMenu.addMenuListener(new MenuAdapter() {
			@Override
            public void menuShown(MenuEvent e) {
				Menu menu = (Menu)e.widget;
				MenuItem[] items = menu.getItems();
				int count = table.getSelectionCount();
				items[2].setEnabled(count != 0); // edit
				items[3].setEnabled(count != 0); // delete
			}
		});

		//New
		MenuItem item = new MenuItem(popUpMenu, SWT.CASCADE);
		item.setText("&New...\tCtrl+N");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				newEntry();
			}
		});

		new MenuItem(popUpMenu, SWT.SEPARATOR);

		//Edit
		item = new MenuItem(popUpMenu, SWT.CASCADE);
		item.setText("&Edit...\tCtrl+E");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items.length == 0) {
					return;
				}
				editEntry(items[0]);
			}
		});
		//Delete
		item = new MenuItem(popUpMenu, SWT.CASCADE);

		item.setText(deleteAction.getText());
		item.setAccelerator(deleteAction.getAccelerator());
		item.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
			    deleteAction.run();
			}
		});

		new MenuItem(popUpMenu, SWT.SEPARATOR);

		return popUpMenu;
	}

	private void editEntry(TableItem item) {
		DataEntryDialog dialog = new DataEntryDialog(table.getShell());
		dialog.setLabels(editColumnNames);
		String[] values = new String[table.getColumnCount()];
		for (int i = 0; i < values.length; i++) {
			values[i] = item.getText(i);
		}
		dialog.setValues(values);
		values = dialog.open();
		if (values != null) {
			item.setText(values);
			try {
			    tweakSymbol(item);
			} catch (Exception e) {
			    Exception x = new Exception(e.getMessage()+" \nOnly one of these symbols will actually be displayed.");
			    ErrorUI.getDefault().signalWarning(x);
			}
			setDirty(true);
		}
	}
	/**
	 * Updates the symbol table to include the symbol described by a table item.
	 * Throws an exception if an overlapping symbol already exists.
	 * Note that the new symbol will be created even if an exception is thrown.
	 * @param item
	 */
	void tweakSymbol(TableItem item) throws Exception {
		//{"Symbol", "Name", "ASCII", "Shortcut","HTML","Family","Unicode (hex)","Status"};
		//{"Family","Name","Ascii","Shortcut","HTML","Unicode (hex)","Unicode Symbol"};
	    Symbol sym=null;
	    String ascii = item.getText(2);
	    if (StringManipulation.isWhitespace(ascii)) {
	        throw new Exception(StringManipulation.convertLineBreak("Tried to create a blank symbol.")
	            +"All symbols must have an ascii definition.");
	    }
	    try {
		    sym = new Symbol(item.getText(1),ascii,
		            item.getText(6),item.getText(4),item.getText(3),
		            item.getText(5));
            makeTableItem(item, sym);
	    } catch (Exception e) {
	        ErrorUI.getDefault().signalError(e);
//	        e.printStackTrace();
	        return;
	    }
	    TableItem[] items = table.getItems();
	    Symbol osym;
	    for (int i =0; i<items.length; i++) {
	        if (items[i]==item) {
	        	continue;
	        }
	        osym = (Symbol) items[i].getData();
	        if (osym.overlap(sym)) {
	            // TODO (minor) give a different warning message (or none?) when it appears that the overlap is just setting up multiple shortcuts to one symbol
	            throw new ScriptingException(StringManipulation.convertLineBreak("Overlapping symbol!")
	                    +sym.name+" ("+sym.ascii+") overlaps with "+osym.name+" ("+osym.ascii+").");
	        }
	    }
	}


    public Action deleteAction;

    void makeActions() {
        final SymbolTableEditor editor = this;
		deleteAction = new Action() {
            @Override
            public void run() {
				TableItem[] items = table.getSelection();
				if (items.length == 0) {
					return;
				}
				items[0].dispose();
				editor.setDirty(true);
            }
		};
		deleteAction.setText("Delete");
	}

    @Override
    public void dispose() {
    	if (bigFont!=null) {
    		bigFont.dispose();
    	}
    	bigFont=null;
    	super.dispose();
    }
}
