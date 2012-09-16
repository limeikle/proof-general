/*
 *  $RCSfile: TPWrapperServerThread.java,v $
 *
 *  Created on 17 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.tpwrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;

/**
 * Worker thread for TPWrapper
 * @author Daniel Winterstein
 */
public class TPWrapperServerThread extends Thread {

	Socket socket = null;
	Process process;
	String[] launchCommand;
	boolean alive = true;


	public TPWrapperServerThread(Socket socket,String[] launchCommand) {
		super();
		this.socket = socket;
		this.launchCommand = launchCommand; // EI
	}

	public void run() {
		BufferedWriter processWriter = null;
		BufferedReader processReader = null;
		BufferedWriter socketWriter = null;
		BufferedReader socketReader = null;
	  try {
	  	System.out.println("Launching "+launchCommand[0]);
	    process = Runtime.getRuntime().exec(launchCommand);

		processWriter = new BufferedWriter(new OutputStreamWriter(
				process.getOutputStream() ));
		processReader = new BufferedReader(new InputStreamReader(
				process.getInputStream() ));

	    socketWriter = new BufferedWriter(new OutputStreamWriter(
	    		socket.getOutputStream() ));
	    socketReader = new BufferedReader(new InputStreamReader(
				socket.getInputStream() ));

	    Pipe pipeOut = new Pipe(processReader,socketWriter,"TP -> ");
	    Pipe pipeIn = new Pipe(socketReader,processWriter,"   <- ");
	    pipeOut.start();
	    pipeIn.start();
	    while(alive) {
	    	sleep(10);
	    }

	  } catch (Exception e) {
	  	e.printStackTrace();
	  	//TODO log errors
	  } finally {
	    // shut down
	  	try {
	  		System.out.println("Closing socket!");
	  		if (processWriter != null) {
				processWriter.close();
			}
	  		if (processReader != null) {
				processReader.close();
			}
	  		if (socketWriter != null) {
				socketWriter.close();
			}
	  		if (socketReader != null) {
				socketReader.close();
			}
	  		socket.close();
	  	} catch (Exception e) {/*ignore*/}
	  }
	}
	/**
	 * This pipe links a reader and a writer together,
	 * All that one reads, the other writes.
	   @author Daniel Winterstein
	 */
	class Pipe extends Thread {
		BufferedWriter writer; BufferedReader reader;
		String prefix;
		/**
		 * Create a pipe linking a writer to a reader.
		 * @param reader
		 * @param writer
		 * @param prefix - Used to produce output for tracing. Set to null to run quietly.
		 */
		public Pipe(BufferedReader reader,BufferedWriter writer,String prefix) {
			this.writer=writer;
			this.reader=reader;
			this.prefix=prefix;
		}
		public void run() {
		  try {
			boolean eof = false;
			while(eof != true) {
				String line = reader.readLine();
				if (line != null) {
					if (line.indexOf(PGIPSyntax.INTERRUPTPROVER) != -1) {
						// TODO: catch interrupt messages and handle appropriately
						char i = 3;
						writer.write(i);
						writer.flush();
					}
					if (prefix != null) {
						System.out.println(prefix+line);
					}
					writer.write(line);
					writer.newLine();
					writer.flush();
				} else {
					eof = true;
				}
			}
		  } catch (java.io.IOException io) {
			  System.out.println("IO: "+io.getMessage());
			  alive = false;
		  } catch (Exception e) {
		  	e.printStackTrace();
			alive = false;
		  }
		}
	}


}
