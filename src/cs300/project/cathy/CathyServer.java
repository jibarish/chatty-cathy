/**
 * Roger Rush
 * Professor Xie
 * CS 300: Elements of Software Engineering / Spring 2014
 * June 2014
 */

package cs300.project.cathy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Properties;
import javax.swing.*;

/**
 * A multi-threaded server for an instant messaging and chatroom service.
 * Clients send connection requests and are each given a Handler thread.
 * Each client logs in using a username and password, and can then send
 * public messages which are broadcast to all other clients, private
 * messages that are displayed only by designated recipient, and can
 * request records of all previous public conversations of that user.
 * Each client is notified when a user signs on or off so it can maintain
 * its buddy list of users currently online.
 */
public class CathyServer {

	// port number
    private static final int PORT = 4444;
    // dictionary of all registered users (String username : String password)
    private static Properties users = new Properties();
    // list of users currently online (String username)
    private static DefaultListModel names = new DefaultListModel();
    // dictionary of user logs (String username : String messageLog)
    private static Properties convos = new Properties();
    // hashset of PrintWriters of users currently online
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    /**
     * The main method listens on a port and creates a handler thread for each
     * connecting client.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The Cathy Chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A Handler thread is instantiated to serve each client that connects.
     */
    private static class Handler extends Thread {

    	// username of connecting client
        private String name;
        // corresponding password
        private String password;
        // connecting socket
        private Socket socket;
        // input
        private BufferedReader in;
        // output
        private PrintWriter out;

        /**
         * Gets and stores socket.  Main control script is run().
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Script sets up i/o streams, prompts client for login info,
         * and goes into a listening loop for messages and client requests.
         */
        public void run() {
            try {

                // creates i/o streams for socket
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // event loop prompts client for login information and
                // logs user in
                while (true) {
                	// get username
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    // get password
                    out.println("SUBMITPASS");
                    password = in.readLine();
                    if (name == null) {
                        return;
                    }
                    // create new account if new user
                    if (!users.containsKey(name)) {
                    	users.put(name, password);
                    }
                    // reject password if wrong
                    if (!(users.get(name).equals(password))) {
                    	out.println("WRONGPASS");
                    } else {
                    	// add username to list of users currently online
                    	synchronized (names) {
                    		if (!names.contains(name)) {
                    			names.addElement(name);
                    			break;
                    		} else {
                    			// notify client that requested username is already logged in
                    			// and may not log in again simultaneously
                    			out.println("DUPLICATE");
                    		}
                    	}
                    }
                }

                // notify client of successful connection
                out.println("NAMEACCEPTED");
                // add output stream to writers
                writers.add(out);
                // if new user add username to convos dictionary
                if (!convos.containsKey(name)) {
                	convos.put(name, "");
                }
                // send user current buddy list
    			Object[] nameStrings = names.toArray();
    			for (Object aName : nameStrings) {
    				out.println("BUDDON " + aName);
    			}
    			// notify all clients that this user has signed on
    			// so they can update their buddy lists
            	for (PrintWriter writer : writers) {
                    writer.println("BUDDON " + name);
            	}

            	// sit and listen for messages and client requests
                while (true) {
                	// get input
                    String input = in.readLine();
                    // handle cases:
                    // null
                    if (input == null) {
                        return;
                    } else if (input.startsWith("PUBLIC")) {
                    	// PUBLIC MESSAGE
                    	String message = input.substring(7);
                    	// broadcast to all clients
                    	for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + message);
                        }
                    	// add to convo logs of all online users
            			Object[] nameStrngs = names.toArray();
            			for (Object aName : nameStrngs) {
            				convos.setProperty("" + aName, ("" + convos.get(aName)).concat(name + ": " + message + "\n"));
            			}
                    } else if (input.startsWith("PRIVATE")) {
                    	// PRIVATE MESSAGE
                    	String recipient = input.substring(8);
                    	String pMessage = in.readLine();
                    	if (pMessage == null) {
                    		return;
                    	} else {
                    		// send message to all clients along with the recipient's name
                    		// it is the client's responsibility to display the message if it is
                    		// intended for that client's user and discard it otherwise
                        	for (PrintWriter writer : writers) {
                                writer.println("PMESSAGE " + recipient);
                                writer.println(name + ": " + pMessage);
                        	}
                    	}
                    } else if (input.startsWith("LOGS")) {
                    	// RETRIEVE LOGS REQUEST
                    	String userLogs = "" + convos.get(name);
                    	out.println("LOGS " + userLogs + " END");
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
            	// this client has left the building
                // notify all clients that this user has signed off
            	// so they can update their buddy lists
             	for (PrintWriter writer : writers) {
                     writer.println("BUDDOFF " + name);
             	}
             	// remove user from currently online list
                if (name != null) {
                    names.removeElement(name);
                }
                // remove writer
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
