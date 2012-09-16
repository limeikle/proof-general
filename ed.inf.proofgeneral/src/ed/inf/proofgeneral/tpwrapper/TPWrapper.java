/*
 *  $RCSfile: TPWrapper.java,v $
 *
 *  Created on 17 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.tpwrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocketFactory;


/**
 * This class wraps a command line prover, enabling it to work via sockets, which
 * allows you to provide theorem prover services over the net.
 *
 * <pre>
 * Usage: java TPWrapper [-secure] portNumber launchCommand
 *
 * Options
 *		-secure		use SSL sockets
 *
 * Notes
 *		- this program will serve as many requests as it gets,
 *        starting separate threads/processes for each session
 *      - launchCommand is fed directly into Runtime.getRuntime().exec,  which can
 *        cause some commands to fail.
 *		- Depending on your firewall configuration, you may need to use SSH
 * 		  tunnelling to use this (this is the case when connecting to any Edinburgh
 * 		  Informatics computers from outside the network).
 * 		- the program runs until it is killed
 *</pre>
 *
 * @author Daniel Winterstein
 */
public class TPWrapper {

	static int port = -1;
	static ServerSocket serverSocket = null;
	static boolean secure = false;
	static String[] launchCommand = null;

	/**
	 * Prints a usage message and quits.
	 * @param exit the exit code to return.
	 */
	public static void printUsage(int exit) {
		System.out.println("Usage: java TPWrapper [-secure] portNumber launchCommand\n");
		System.exit(exit); // Dm
	}

	/**
	 * Launches a new prover wrapper with given arguments.
	 * @param args Usage: TPWrapper [-secure] portNumber launchCommand
	 */
	public static void main(String[] args) {
		ArrayList<String> arg = new ArrayList<String>(Arrays.asList(args));
		System.out.println("TPWrapper for Proof General\n");
		if (arg.size() == 0 || arg.get(0).contains("?")) { printUsage(0); }

		if (arg.get(0).equals("-secure")) {
			secure=true;
			arg.remove(0); // remove 1st argument
		}

		if (arg.size() < 2) { printUsage(1); }

		try {
			port = Integer.parseInt(arg.get(0));
		} catch (NumberFormatException e) {
			System.err.println("Error: port '"+arg.get(0)+"' is not a number.");
			printUsage(1);
		}
		arg.remove(0); // remove argument

		launchCommand = arg.toArray(new String[arg.size()]);

		// DEBUG
		// for(String s : launchCommand) System.out.println("["+s+"]");

		try {
			System.out.println("...listening on "+InetAddress.getLocalHost().getCanonicalHostName()
					+":"+Integer.toString(port));
		} catch (Exception e) { // ignore
		}

		// --- End argument processing ---
		boolean listening = true;

        try {
        	if (secure) {
        		System.err.println("Note: Secure socket support is unsupported (See TPWrapper.java)");
        		serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
        	} else {
        		serverSocket = new ServerSocket(port);
        	}
        } catch (IOException e) {
            System.err.println("Could not listen on port "+Integer.toString(port));
            System.exit(1);
        }

        System.out.println("Listening...");
        while (listening) { // IL (yes!)
        	try {
        		Socket client = serverSocket.accept();
        		System.out.println("...Accepting a new client...");
        		new TPWrapperServerThread(client, launchCommand).start();
        	} catch (SSLException e) {
        		System.err.println("SSL Error... quitting.");
        		e.printStackTrace();
        		System.exit(0);
        	} catch (Exception e) {
        		System.err.println("Server Thread Failure");
        		e.printStackTrace();
        	}
        }
        try {
        	serverSocket.close();
        	System.out.println("Closed.");
        } catch (Exception e) { //ignore
        }
	}

}
