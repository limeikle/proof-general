/*
 *  $RCSfile: Converter.java,v $
 *
 *  Created on 15 Jul 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.pgip2html;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.exception.KnownException;
import ed.inf.utils.file.FileUtils;
/**
 @author Daniel Winterstein
 */
public class Converter {

	/** The prover with which this converter is associated */
	private final Prover prover;
	/** * The compiled HTML style sheet (user preference, per-prover) */
	private Transformer transformerHtml;

	/** * The compiled plaintext style sheet (currently fixed for all provers) */
	private static Transformer transformerPlaintext = 
		compileStylesheet(null,PreferenceNames.PLAINTEXT_STYLESHEET_FILE);
	
	public static final Converter plainConverter = new Converter(null);

	/**
	 * Initialise this PGML converter by preparing the transformers.
	 * Gives an error message if the stylesheet cannot be found, but this will
	 * not break subsequent uses of getDisplayHtml.  Should be called 
	 * in the event of the style sheet being updated. 
	 */
	public void init() { 
		// might be synchronized to avoid multiple entry, but we only update
		// one field.
		if (prover != null) {
			transformerHtml = compileStylesheet(prover.getName(),
					prover.getStringPref(ProverPreferenceNames.PREF_STYLESHEET_FILE));
		}
		if (transformerHtml==null) {
			// a better fall-back than literal XML
			transformerHtml = transformerPlaintext; 
		}
	}
	
	/**
	 * Initialise stylesheet transformers for the given prover.
	 * @param prover
	 */
	public Converter(Prover prover) {
		this.prover = prover;
		init();
	}

	/**
     * @param styleFileName
     * @throws TransformerFactoryConfigurationError
     */
    private static Transformer compileStylesheet(String proverName, String styleFileName)
            throws TransformerFactoryConfigurationError {
       	// FIXME da: string filename should probably be an IPath or similar
   	    try {
  				if (styleFileName==null || styleFileName.equals("")) {
  					throw new Exception("No setting found for PGML style sheet file name.");
  		}
  		File styleFile = FileUtils.findFileExt(proverName,styleFileName);
  		if (styleFile != null) {
  			TransformerFactory factory = TransformerFactory.newInstance();
  			return factory.newTransformer(new StreamSource( styleFile ) );
  		} 
  		throw new Exception("Problem opening style sheet file " + styleFileName);
  		} catch (Exception e)  {
				KnownException ke =
  					new KnownException("Error with PGML style sheet \"" + styleFileName + "\", prover output will be raw.\n"+
  							// da: any way to retrieve the errors reported along the way?  They appear on
  							// stderr, which is OK but not ideal.  This is not the right exn, maybe.
  							((e instanceof TransformerConfigurationException) ?
  										((TransformerConfigurationException) e).getMessageAndLocation() :
  										e.getMessage()));
  			if (ProofGeneralPlugin.isEclipseMode()) {
  				EclipseMethods.errorDialog(ke);
  			} else {
  				System.err.println(ke.getMessage());
  			}
  		}
  		return null;
    }
    
	/**
	 * Given an unparsed text fragment in PGML without layout markup
     * (i.e. no pgmltext or statedisplay elements),
	 * return an HTML string for it by applying the PGML stylesheet,
     * also converting symbols into Unicode forms.
	 * @param str
	 * @return an HTML fragment corresponding to str, with symbols decoded
	 */
    public String getDisplayHtml(String str) {
    	return getTransformedText(str,transformerHtml);
    }
    
	/**
	 * Given an unparsed text fragment in PGML without layout markup
     * (i.e. no pgmltext or statedisplay elements),
	 * return a plaintext string for it by applying the PGML plaintext stylesheet,
     * also converting symbols into Unicode forms.
	 * @param str
	 * @return a plaintext fragment corresponding to str, with symbols decoded
	 */
    public String getPlaintext(String str) {
    	return getTransformedText(str,transformerPlaintext);
    }
    
    public String getPlaintext(Element elt) {
    	return getDisplayText(elt,true,transformerPlaintext);
    }
    
	public String getTransformedText(String str,Transformer transformer) {
		SAXReader reader = new SAXReader();
		Document document;
		try {
			// NB: may have pgmltext markup already, but duplicate has no effect.
			document = reader.read( new StringReader("<pgmltext>"+str+"</pgmltext>"));
			
			str = getDisplayText((Element) document.content().get(0),true,transformer);
			return str;
		} catch (DocumentException e) {
			return e.getLocalizedMessage() + "\n[problem parsing PGMLtext:\n"+str+"]";
		}
	}
	
	public String getDisplayText(Element elt) {
		return getDisplayText(elt,false,transformerHtml);
	}

    /**
     * Apply the PGML style sheet, transforming the parsed XML into HTML,
     * and also converting document symbols into Unicode.  (Interim while
     * we have document symbols in output).
     * @param pgml the element to convert
     * @param disposable true if the event is disposable, i.e. can be modified.
     * 					false if the caller doesn't mind the element being destroyed.
     * @param transformer used for the transformation (and to see if it is HTML or plain text)
     * @return the representation of the given element
     */
    private String getDisplayText(Element pgml, boolean disposable, Transformer transformer) {
    	if (pgml == null) {
    		return null;
    	}
    	Element pgmlcopy = disposable ? pgml : pgml.createCopy();
    	if (disposable) {
    		pgml.detach();
    	}
    	String result;
    	try {
    		result = transformOrDefault(pgmlcopy,transformer);
    	} catch (Exception ex) {
     		System.err.println("Could not pretty-format prover output with PGML stylesheet file");
    		System.err.println(ex.getLocalizedMessage());
    		System.err.println("Input document was:");
    		System.err.println(pgml.asXML());
    		result = pgml.getStringValue().replace("\n","<br/>\n");
    	}
    	return result;
    }

    private String transformOrDefault(Element pgml, Transformer transformer)
    throws TransformerException {
    	if (transformer == null) {
    		// degenerate plain output in case of style file problems
    		return pgml.getStringValue().replace("\n","<br/>\n");
    	}
    	Document document = DocumentHelper.createDocument(pgml);
    	DocumentSource docsource = new DocumentSource(document);
    	DocumentResult docresult = new DocumentResult();
    	synchronized (transformer) { // single-threaded use only
    		transformer.transform(docsource, docresult);
    	}
    	Document transformedDoc = docresult.getDocument();
    	String result;
    	boolean wantHtml = (transformer != transformerPlaintext); 
    	if (wantHtml) {
    		result = transformedDoc.asXML();
    		// FIXME da: check cross-OS compatibility on below.
    		 // Convert CRs into explicit line-breaks (could be in stylesheet?)
        	result = result.replaceFirst("<\\?.+\\?>\n","");  // strip XML header
    		result = result.replace("\n","<br/>\n");
    		result = result.trim();
    	} else {
    		result = transformedDoc.getStringValue();
    	}
    	// Remove non-PGML symbols.  FIXME: remove this when we get
    	// conversion in the stylesheets instead.
    	return prover.getSymbols().useUnicodeForOutput(result,wantHtml);
    }


	/**
	 * Convert the given string to XML, with entities escaped.
	 * @param input
	 * @return input with XML 1.0 entities escaped
	 */
	public static String stringToXml(String input) {
		if (input==null) {
			return "";
		}
		StringWriter wr = new StringWriter();
		XMLWriter xwr = new XMLWriter(wr);  // XFB:
		/*  This method allocates a specific implementation of an xml interface. It is preferable to use the supplied factory classes to create these objects so that the implementation can be changed at runtime. See

    * javax.xml.parsers.DocumentBuilderFactory
    * javax.xml.parsers.SAXParserFactory
    * javax.xml.transform.TransformerFactory
    * org.w3c.dom.Document.createXXXX

	*/
		try {
				xwr.write(input);
				return wr.toString();
		} catch (IOException e) {
				return "";
		}
	}

	// da: as in StorageDocumentProvider
	// protected static final int DEFAULT_FILE_SIZE= 15 * 1024;

/*	da: this is for sending parsescript commands for files which do not
    have associated documents, as a background build task.
    See ProofBuilder.  TODO: Defer for now.
    public static String fileToXML(IFile file) {
		InputStream contentStream = file.getContents();
		// FIXME: encoding
		BufferedReader in =
			new BufferedReader(new InputStreamReader(contentStream), DEFAULT_FILE_SIZE);
		BufferedWriter out = new BufferedWriter(new CharArrayWriter());
		XMLWriter xwr = new XMLWriter(out);
		char[] readBuffer= new char[2048];
		int n = in.read(readBuffer);
		while (n > 0) {
			CDATA cd = new FlyweightCDATA();
			xwr.write(cdata) buffer.append(readBuffer, 0, n);
			n= in.read(readBuffer);
		}


		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLWriter xwr = new XMLWriter(contentStream);

		BufferedReader in =
			new BufferedReader(new InputStreamReader(contentStream, file.get), DE
					FAULT_FILE_SIZE);
	StringBuffer buffer= new StringBuffer(DEFAULT_FILE_SIZE);
	}
*/
}

