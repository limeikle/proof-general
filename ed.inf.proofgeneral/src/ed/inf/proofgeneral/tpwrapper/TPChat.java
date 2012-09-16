/*
 *  $RCSfile: TPChat.java,v $
 *
 *  Created on 17 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.tpwrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 Chat to a socket at a given address and port.
 */
public class TPChat {
	
	/**
	 * Begins a chat to the specific address and port. 
	 * @param args to be taken from command line,
	 * 				usage is <pre>TPChat [hostAddress] [portNumber]</pre>
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: TPChat hostAddress portNumber");
			System.exit(1);
		}
		String hostAddress = args[0];
		int portNumber = Integer.parseInt(args[1]);
				
		Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
        	System.out.println("Trying to connect...");
            socket = new Socket(hostAddress, portNumber);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                                        socket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: "+hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                               + "the connection to: "+hostAddress);
            System.exit(1);
        }
        assert socket != null : "Socket not set";
        assert out != null : "Output stream not set";
        assert in != null : "Input stream not set";
        
        BufferedReader stdIn = new BufferedReader(
                                   new InputStreamReader(System.in));
        String userInput;
        System.out.println("connected...");
        try {
        String lineIn = "";
        while ((userInput = stdIn.readLine()) != null && lineIn != null) {
        	out.println(userInput);
        	lineIn = in.readLine();        	
        	System.out.println("TP: " + lineIn);
        }

        out.close();
        in.close();
        stdIn.close();
        socket.close();
        } catch (Exception e) {e.printStackTrace();}
        System.out.println("Chat Finished.");
	}
}
