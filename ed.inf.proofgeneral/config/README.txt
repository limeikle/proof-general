
This directory contains files used to configure Proof General Eclipse.

These files can be edited by hand. This should be straightforward.
Please be aware though, that some of their structure and settings 
are expected by the code, and changes *may* produce unforseen errors.


These include:

	Preference files for both Proof General and for its provers:
		ProofGeneralPrefs.xml
		colourPrefs.xml
		BlockedMessagesPrefsPage.xml
		IsabellePrefs.xml
			These preferences define how PG talks to Isabelle.
		IsabelleConfigPrefs.xml	
			These preferences are settings of the underlying prover.
			They are exposed in Eclipse for user-friendly editing.
			Be careful supplying defaults for these settings - PG will
			only tell the theorem prover about non-default settings, so
			an inaccurate default setting would lead to bugs and confusion.
		
	These preference pages are referenced from plugin.xml. 
	See PrefPagesBackend for details on creating new preference pages.

	defaultSymbolTable.sym
		The default symbol table used by proof general for supporting mathematical symbols.

	pgmlStyleFile.xsl
		A style file used to style prover output. Converts PGML into HTML.

	IsabelleKeywords.txt
		Lists of keywords for the provers (used in syntax highlighting).

    IsabelleTooltips.xml
		A related file for keyword help.

	
	