import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;

/**
 * 
 * Peers contains information of its finger table and files associated with it
 * 
 * @author mk7293
 *
 */
public class Peers implements Runnable {

	private int guid;
	private String serverIPAddress;
	private int serverPort;
//	private Socket socket;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	private JSONRPC2Session session;
	private JSONRPC2Request jsonRequest;
	private JSONRPC2Response jsonResponse;
	private boolean isNodeOnline = false;

	public Peers(int guid, String ipAddress, int port) {

		this.guid = guid;
		this.serverIPAddress = ipAddress;
		this.serverPort = port;
		while (true) {
			Scanner scanner = new Scanner(System.in);

			try {

				System.out.println("Guid: " + guid);

				System.out.println(
						"Chord DHT Menu \n 1. Join Network \n " + "2. Leave Network \n 3. Add File \n 4. Show Files \n "
								+ "5. Search File \n 6. Show Finger Table \n ");

				int option = scanner.nextInt();

				switch (option) {
				case 1:
					joinNetwork();
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
					break;
				case 6:
					System.exit(0);
					break;
				}

			} catch (InputMismatchException e) {
				System.out.println("Please provide the correct option number");
			}
		}

	}

	private void connectToSocket() {
		URL serverURL = null;
		try {
			serverURL = new URL("http://" + serverIPAddress + ":" + serverPort + "/");
		} catch (MalformedURLException e) {
			System.err.println("MalformedURLException occured");
		}
		System.out.println("To: " +serverURL.toString());
		session = new JSONRPC2Session(serverURL);
		System.out.println("Connect");
	}

	private void disconnectFromSocket() {
		
	}

	/**
	 * Join into the network
	 */
	private void joinNetwork() {
		System.out.println("INside method");
		if (!isNodeOnline) {
			System.out.println("Insider Node");
			connectToSocket();
			ArrayList<Object> list = new ArrayList<>();
			list.add(guid);
			jsonRequest = new JSONRPC2Request("socket", list, 0);
			jsonResponse = null;
			try {
				jsonResponse = session.send(jsonRequest);
			} catch (JSONRPC2SessionException e) {
				System.err.println(e.getMessage());
			}
			
			System.out.println("jsonResponse.indicatesSuccess():: " + jsonResponse.indicatesSuccess());
			if (jsonResponse.indicatesSuccess()) {
				System.out.println(jsonResponse.getResult());
			} else {
				System.out.println("Error");
			}
		}
		System.out.println("Exit Method");
	}

	/**
	 * Leave the Network
	 */
	private void leaveNetwork() {

	}

	/**
	 * Add the files into the network when the hashed file code matches with its
	 * peer id
	 */
	private void addFile() {

	}

	/**
	 * Search for the file by lookup on the finger table
	 */
	private void searchFile() {

	}

	/**
	 * Transfer the file into the successor nodes when its goes offline
	 */
	private void transferFile() {

	}

	/**
	 * Get the file from the sucessor when the nodes comes online
	 */
	private void getFiles() {

	}

	/**
	 * Update the finger table based on the peers activity
	 */
	private void constructFingerTable() {

	}

	public void run() {

	}

	/**
	 * Main method will contain a list of menu's for the current peer to perform
	 * operation of their choice
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			System.err.println("Usage: GUID, ServerIPAddress");
		}

		new Peers(Integer.parseInt(args[0]), args[1], 8000);
	}

}


