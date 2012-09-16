/*
 *  $RCSfile: ExternalLazyParser.java,v $
 *
 *  Created on 01 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lazyparser;
import java.util.Observable;

import org.dom4j.Element;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.IProverStateObserver;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.datastruct.NumericStringUtils;


/**
 * A lazy parser based on using the PGIP <parsescript> command
 * and the active prover
 * @author Daniel Winterstein
 */

// TODO da: the external parser should be invoked during prover+user idle time,
// to automatically parse.  We should signal parse errors in the standard Eclipse
// way (wavy lines), not dumping error messages in the output buffer and
// certainly not brining up dialog boxes!!

public class ExternalLazyParser extends Parser implements IPGIPListener, IProverStateObserver {

	/**
	 * The external parser is potentially slow on large inputs - it should not be allowed to block
	 */
	@Override
    public boolean isSlow() {
		return true;
	}

    SessionManager sm;
    /**
     *
     */
    public ExternalLazyParser(SessionManager sm) {
        super(sm.proverInfo.syntax);
        this.sm = sm;
        sm.addListener(this);
        sm.getProverState().addObserver(this);
    }
    
    @Override
    public void dispose() {
        sm.removeListener(this);
        sm.getProverState().deleteObserver(this);
    	super.dispose();
    }


    private volatile boolean waiting = false;
    PGIPEvent response;

    
	/**
	 * In case a shutdown occurs, turn off waiting and notify threads.
	 */
	public void update(Observable o,  Object ignored) {
		assert o instanceof ProverState : "Wrong Observable type";
		ProverState ps = (ProverState) o;
		if (!ps.isAlive()) {
			finish();
    		response = null;
		}
	}
	
	/**
	 * Finish parse: turn off waiting and notify threads.
	 */
	private synchronized void finish(){
		waiting = false;
		this.notifyAll();
	}

    /**
     * Wakes up the waiting main thread when a CommandProcessed or CCError event is received
     */
    // TODO da: this needs cleaning up and moving into command queue as a general facility
    	// da: consider expanding the scope of the lock here to the whole method rather than
        // leaves inside tests.  This means that we'll process one event fully before
    	// others enter here too.  That seems sensible unless we really lose speed on it
    	// (but big parses seemed to cause bugs here anyway).
    public void pgipEvent(PGIPEvent event) {
		    //System.out.println("ELP: "+event);
        synchronized (this) {
        	if (!waiting) {
        		return;
        	}
        }
        if (event.cause!=this) {
        	if (event.cause!=cause) {
        		//this could happen if things get queued... probably hide it and don't worry if it ever shows
        		System.err.println("ExternalLazyParser got event caused by "+event.cause+" when waiting for its own request due to "+cause+"; cancelling.  event is "+event);
        		return;
        	}
      		return;
        }
        if (event instanceof CommandProcessed) {
        	finish();
        	return;
        }
        if (event instanceof PGIPReady) {
        	return; // don't set this as the response
        }
        if (event instanceof PGIPIncoming || event instanceof PGIPError) {
        	synchronized (this) {
        		response = event;
        		if (!sm.getProverState().isAlive() || event instanceof SessionManager.TimeOutError) {
        			waiting = false; // the prover has died - no point waiting for a ready message
        			//System.out.println("NOTIFYING on "+event);
        			this.notifyAll();  // da: made notifyAll, unnecessary?
        			return;
        		}
          }
        }
        if (event instanceof CommandCausedErrorEvent) {
        	//only notify when CommandProcessed received
//        	  || event instanceof PGIPShutdown) {   //also catch this one -AH
        	synchronized (this) {
        		response = event;
//        		waiting = false;
//        		this.notify();
        	}
        	return;
        }
    }


    public static class ExternalParsingInterruptedException extends ParsingInterruptedException {
		public ExternalParsingInterruptedException(String text) {
			super(text);
		}
    }

    /**
     * Send text out to the theorem prover and wait for the parse results
     * @param text
     * @return parseResult element
     * @throws ScriptingException
     * @throws ExternalParsingInterruptedException
     */
     // FIXME da: should check sync here.
    @Override
    public Element dumbParseText(String text)
    throws ScriptingException, ParsingInterruptedException {
    	if (!sm.isEmptyQueue() || waiting) { // da: added waiting here: sometimes loops (prob below)
//  		// TODO something - wait?
//  		Object o[] = sm.commandQueue.toArray();
//  		int fs = sm.firingSequence.get();
//  		int fs2 = sm.firingQueueSequence;
//  		System.err.println("ExternalLazyParser can't run:\n  parsing '"+text+"'\n  queue length "+o.length+"\n  firing seq lag "+(fs-fs2));
//  		//delay 1s then check again
//  		General.sleep(1000);
//  		if (!sm.isEmptyQueueAndEvents())
    		throw new ScriptingException("Queuing error: External parser cannot be called if there is a command queue.");
    	}
    	// protect against being locked by dead sessions
    	sm.setTimeout(ProofGeneralPlugin.getIntegerPref(PreferenceNames.PREF_TIME_OUT), false);
    	waiting = true;
//  	boolean smBgThread = sm.backgroundThread; // store these valuse so we can reset them
//  	boolean smLogging = sm.logging;
//  	IRegion r;
    	CmdElement parseCmd = new CmdElement(PGIPSyntax.PARSESCRIPT);
//  	PGIPListener oldPrivateListener = sm.privateListener; //restore the previous private listener state if there is one, don't blindly reset to null  -AH
    	try {
//  		sm.backgroundThread = true; // need to run the session manager in a bg thread 'cos we're going to lock the display thread
//  		sm.logging = false; // switch off message logging
//  		sm.privateListener = this; // hide this conversation from everyone else
    		parseCmd.setText(text);
    		//System.out.println("ELP: queing and waiting on parse "+parseCmd.asXML());

    		sm.queueCommand(parseCmd, this, Boolean.TRUE, new MutableObject(this));  //changed so that WE are the cause, not the SendCommandAction; b/c we want to listen to results, not SCA
    		int count = -1;
    		while (waiting && count < 60) {
    			if (count++>0 && count%10==0) {
    				System.err.println(NumericStringUtils.makeDateString()+"  ExternalLazyParser still waiting for response after "+count+"s, request length is "+text.length());
    			}
    			// FIXME da: I think this loop can hang sometimes.  Race condition here?
    			this.wait(1000); 	// now wait...
    		}
			if (waiting) {
				System.err.println(NumericStringUtils.makeDateString()+"  ExternalLazyParser given up waiting");
				waiting = false;
			}
    		//System.out.println("ELP: done waiting on parse "+parseCmd.asXML());
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		waiting = false;
    		throw new ScriptingException(ex.getMessage());
    	} finally {
//  		sm.backgroundThread = smBgThread; // switch session manager output back to normal thread
//  		sm.logging = smLogging;
//  		sm.privateListener = oldPrivateListener; // release the session manager to talk to everyone
    	}
    	if (response == null) {
    		throw new ScriptingException("Parsing Protocol Error: parse command did not get a response");
    	}
    	if (response instanceof PGIPError) {
    		String es = response.parseTree.getText();
    		if (es.indexOf(SessionManager.TIMEOUT_ERROR_TEXT) != -1) {
    			throw new ScriptingException(response.getText());
    		}
    		//  TODO this is a violation of protocol, which says we should always get a parseresult
    		// but it seems to occur
    		// FIXME da: this case does seem to happen in the code, I agree, but I'm not convinced
    		// it happens in the prover!
    		throw new UnparseableException(es);
    	}
    	if (response instanceof CommandCausedErrorEvent) {
    		CommandCausedErrorEvent cce = (CommandCausedErrorEvent) response;
    		throw new ExternalParsingInterruptedException(cce.getText());
    	}
    	if (!"parseresult".equals(response.type)) {  //put string first becaues type is sometimes null (shouldn't be, but...) -AH
    		throw new ScriptingException("Parsing Protocol Error - unknown response: "+response.getText());
    	}
    	//System.out.println("SENT PARSE REQUEST\n"+parseCmd.asXML()+"\n----- response is\n"+response.parseTree.asXML()+"\n\n");
    	Element parseResult = (Element) response.parseTree.elements().get(0);
    	return parseResult;
    }
}
