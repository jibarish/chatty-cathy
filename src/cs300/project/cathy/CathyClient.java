/**
 * Roger Rush
 * Professor Xie
 * CS 300: Elements of Software Engineering / Spring 2014
 * June 2014
 */

package cs300.project.cathy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.miginfocom.swing.MigLayout;

/**
 * A swing client that connects to a Cathy Chat server by logging
 * in with a username and password.  By default the IP address is
 * localhost but can be changed in settings.  Through GUI user can
 * enter and send public messages which are displayed to all other
 * online users, private messages which are displayed only by
 * designated recipient, and can retrieve logs of all previous
 * public conversations which are saved to user's computer in
 * a text file timestamped and labeled with the current user's
 * username.
 */
public class CathyClient {

	// IP address of server
	private String serverAddress = "localhost";
	// port number (same for all clients)
	private static final int PORT = 4444;
	// i/o streams
    private BufferedReader in;
    private PrintWriter out;
    // current user identity
    private String username;
    // buddy list model
    private DefaultListModel listModel;
    // GUI elements
    private JFrame frame;
    private JPanel mainPanel;
    private JTextField publicTextField;
    private JTextField privateTextField;
    private JTextArea publicTextArea;
    private JTextArea privateTextArea;
    private JList list;
	private JLabel publicLabel;
	private JLabel privateLabel;
	private JLabel buddyListLabel;

	/**
	 * Constructor initializes all data fields and lays out GUI.
	 */
    public CathyClient() {

        // set up frame
    	frame = new JFrame("Chatty Cathy");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 764, 300);

		// set up panel
		mainPanel = new JPanel();
		mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		frame.setContentPane(mainPanel);
		mainPanel.setLayout(new MigLayout("", "[grow][100][grow]", "[][grow][][]"));
		mainPanel.setVisible(true);

		// labels
		// public convo
		publicLabel = new JLabel("Public");
		mainPanel.add(publicLabel, "cell 0 0,alignx center");
		// private convo
		privateLabel = new JLabel("Private");
		mainPanel.add(privateLabel, "cell 2 0,alignx center");
		// buddy (currently online) list
		buddyListLabel = new JLabel("Currently Online");
		mainPanel.add(buddyListLabel, "cell 1 0,alignx center");

		// text areas for convo display
		// public
		publicTextArea = new JTextArea();
		publicTextArea.setEditable(false);
		mainPanel.add(new JScrollPane(publicTextArea), "cell 0 1,grow");
		// private
		privateTextArea = new JTextArea();
		privateTextArea.setEditable(false);
		mainPanel.add(new JScrollPane(privateTextArea), "cell 2 1,grow");

		// buddy (currently online) list
		listModel = new DefaultListModel();
		list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(10);
        JScrollPane listScrollPane = new JScrollPane(list);
		mainPanel.add(listScrollPane, "cell 1 1,grow");

		// textfields for message entry
		// public
		publicTextField = new JTextField();
		mainPanel.add(publicTextField, "cell 0 2,growx");
		publicTextField.setColumns(10);
		// private
		privateTextField = new JTextField();
		mainPanel.add(privateTextField, "cell 2 2,growx");
		privateTextField.setColumns(10);

		// buttons
		// send (public)
		JButton sendPublicButton = new JButton("Send");
		sendPublicButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendPublicMessage();
			}
		});
		mainPanel.add(sendPublicButton, "cell 0 3,growx");
		// send (private)
		JButton sendPrivateButton = new JButton("Send");
		sendPrivateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendPrivateMessage();
			}
		});
		mainPanel.add(sendPrivateButton, "cell 2 3,growx");
		// logs
		JButton logsButton = new JButton("Logs");
		logsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				out.println("LOGS");
			}
		});
		mainPanel.add(logsButton, "cell 1 3,growx");
		// settings
		JButton settingsButton = new JButton("Settings");
		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				serverAddress = getServerAddress();
			}
		});
		mainPanel.add(settingsButton, "cell 1 2,growx");

        // non-button listeners
		// public textfield return
        publicTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	sendPublicMessage();
            }
        });
        // private textfield return
        privateTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if (list.isSelectionEmpty()) {
            		JOptionPane.showMessageDialog(frame, "Please select a recipient to send a private message.");
            	} else {
            		sendPrivateMessage();
            	}
            }
        });
    }


	/**
	 * Sends contents of public message textfield to server.
	 */
    private void sendPublicMessage() {
        out.println("PUBLIC " + publicTextField.getText());
        publicTextField.setText("");
    }

	/**
	 * Sends contents of private message textfield to server
	 * along with identification of recipient.
	 */
    private void sendPrivateMessage() {
		String recipient = "" + listModel.get(list.getSelectedIndex());
		String pMessage = privateTextField.getText();
		out.println("PRIVATE " + recipient);
		out.println(pMessage);
		privateTextArea.append(username + " -> " + recipient + ": " + pMessage + "\n");
		privateTextField.setText("");
    }

	/**
	 * Allows user to change IP address of server.  Called when user
	 * presses "Settings" button.
	 */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Settings",
            JOptionPane.QUESTION_MESSAGE);
    }

	/**
	 * Modal dialog prompts user to enter username.  User may create
	 * a new account by entering a new username, or sign into an
	 * existing account by entering an existing username.  Is called
	 * automatically when program is opened.
	 */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter username to login or create a new account: ",
                "Chatty Cathy Login",
                JOptionPane.QUESTION_MESSAGE);
    }

	/**
	 * Modal dialog follows username entry and prompts user to enter
	 * password.  If a new account is being created then this will
	 * set its password.  Otherwise this password must match the
	 * entered username.
	 */
    private String getPass() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter password: ",
                "Chatty Cathy Login",
                JOptionPane.QUESTION_MESSAGE);
    }

	/**
	 * The primary event loop.  Makes connection with server and then
	 * sits and waits for message updates.
	 */
    private void run() throws IOException {

        // connect to server and initialize i/o streams
        Socket socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // process messages from server
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
            	// send username
            	username = getName();
                out.println(username);
            } else if (line.startsWith("SUBMITPASS")) {
            	// send password
                out.println(getPass());
            } else if (line.startsWith("WRONGPASS")) {
            	// invalid password, try again
                JOptionPane.showConfirmDialog(frame, "Incorrect password, try again.");
            } else if (line.startsWith("DUPLICATE")) {
            	// username already logged onto server, try with a different name
                JOptionPane.showConfirmDialog(frame, "This user is already logged in, try again.");
            } else if (line.startsWith("NAMEACCEPTED")) {
            	// name accepted, now user may enter text
                publicTextField.setEditable(true);
                privateTextField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
            	// receive public message
            	publicTextArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("PMESSAGE")) {
            	// receive private message
            	// only display if recipient is current user!
            	String recipient = line.substring(9);
            	if (recipient.equals(username)) {
            		privateTextArea.append(in.readLine() + "\n");
            	}
            }  else if (line.startsWith("BUDDON")) {
            	// user signed on, update buddy list
            	String userOnline = line.substring(7);
            	if (!listModel.contains(userOnline)) {
            		listModel.addElement(userOnline);
            	}
            } else if (line.startsWith("BUDDOFF")) {
            	// user signed off, update buddy list
            	String userOffline = line.substring(8);
            	if (listModel.contains(userOffline)) {
            		listModel.removeElement(userOffline);
            	}
            } else if (line.startsWith("LOGS")) {
            	// receive logs, which are saved as .txt file
            	String myLogs = "";
            	String nextLine = line.substring(5);
            	do {
            		myLogs = myLogs.concat(nextLine + "\n");
            		nextLine = in.readLine();
            	} while (!nextLine.endsWith("END"));
            	System.out.println(line);
            	System.out.println(myLogs);
            	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
            	Date date = new Date();
            	PrintWriter outfile = new PrintWriter(username + "'s public message logs " + dateFormat.format(date) + ".txt");
            	outfile.println(myLogs);
            	outfile.close();
            }
        }
    }

	/**
	 * Main method creates client, displays GUI, and calls the
	 * primary event loop, run().
	 */
    public static void main(String[] args) throws Exception {
        CathyClient client = new CathyClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
