package ed.inf.proofgeneral.sessionmanager;

import java.io.BufferedReader;
import java.net.SocketException;

import ed.inf.utils.eclipse.ErrorUI;

/**
 * Stream gobblers handle TP output in a separate thread
 *  - otherwise things hang.
 *  Output is re-routed to processOutput() whenever a line-end is received.
 */
class StreamGobbler extends Thread {

	private final SessionManager parent;
	BufferedReader reader;
	boolean notifyOnStop = false;

	StreamGobbler(String name, BufferedReader reader, SessionManager parent, boolean notifyOnStop) {
		super("SteamGobbler-Thread-"+name);
		this.reader = reader;
		this.parent = parent;
		this.notifyOnStop = notifyOnStop;
		setDaemon(true);
		start();
	}

	public boolean bytesAvailable() {
		//TODO should set this up as non-blocking (ready blocks, dammit)
		return false;
//		try {
//		System.out.println("waiting");
//		System.out.println("the tp output stream is "+(reader.ready() ? "active" : "quiet"));
//		return reader.ready();
//		} catch (IOException e) {
//		return false;
//		}
	}

	public void run() {
		try {
			String line="";
			while ((line = reader.readLine()) != null) {
				if (parent.consoleoutput!=null) {
					parent.consoleoutput.writeoutput(line+"\n");
				}
				parent.processOutput(line);
				//TODO can remove all uses of dontParseInDisplayThread
				//no need ever to run it in the display thread... probably a bad idea in fact for speed reasons
//				if (parent.dontProcessOutputInDisplayThread) {
//				parent.processOutput(line);
//				} else {
//				org.eclipse.swt.widgets.Display.getDefault().asyncExec( new switchThread(parent,line) );
//				}
			}
			reader.close();
			if (notifyOnStop) {
				parent.noteStreamStopped();
			}
		} catch (SocketException e) {
			ErrorUI.getDefault().signalWarning(e);
		} catch (Exception e) {
			if (!isInterrupted()) {
				e.printStackTrace();
			}
			try {
				reader.close();
			} catch (Exception e2) {/*ignore*/}
		}
	}


	public void interrupt() {
		super.interrupt();
		//this blocks, deadlocking with readLine above
		//try {reader.close();} catch (Exception e) {}
		//should close on destroy... but if it doesn't consider using AutomaticInputStream -AH
	}
}