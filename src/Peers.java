import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.TreeMap;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;

/**
 * 
 * Peers contains information of its finger table and files associated with it
 * 
 * @author mk7293
 *
 */
public class Peers implements Runnable {

	private int guid;
	private static int requestId;
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
		System.out.println("To: " + serverURL.toString());
		session = new JSONRPC2Session(serverURL);
		System.out.println("Connect");
	}

//	private void disconnectFromSocket() {
//		session.getOptions()
//	}

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
			list.add(session.getURL().getHost());
			jsonRequest = new JSONRPC2Request("join", list, requestId++);
			jsonResponse = null;
			try {
				jsonResponse = session.send(jsonRequest);
			} catch (JSONRPC2SessionException e) {
				System.err.println(e.getMessage());
			}

			String response = "";
			if (jsonResponse.indicatesSuccess()) {
				response = (String) jsonResponse.getResult();
				System.out.println("JSONResponse from server: " + response);
			} else {
				System.out.println("Error");
			}

			if (response.equalsIgnoreCase("Welcome GUID: " + guid)) {
				System.out.println("GUID: " + guid + " joined along with peers");
				isNodeOnline = true;
				
				list.clear();
				list.add(this);
				list.add(guid);
				jsonRequest = new JSONRPC2Request("getliveNodes", list, requestId++);
				jsonResponse = null;
				try {
					jsonResponse = session.send(jsonRequest);
				} catch (JSONRPC2SessionException e) {
					System.err.println(e.getMessage());
				}

				response = "";
				if (jsonResponse.indicatesSuccess()) {
					response = (String) jsonResponse.getResult();
					System.out.println("JSONResponse from server: " + response);
				} else {
					System.out.println("Error");
				}
			}

		} else {
			System.out.println("Network '" + guid + "' already joined ");
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
	public void constructFingerTable(int guid, TreeMap<Integer, InetAddress> hashMap) {

	}
	
	public void run() {

		try {
			ServerSocket serverSocket = new ServerSocket(7000);
			Dispatcher dispatcher = new Dispatcher();
			dispatcher.register(new PeerHandler.SocketHandler());
			

			while (true) {
				Socket socket = serverSocket.accept();
				PrintWriter outputStream = new PrintWriter(socket.getOutputStream());
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				String contentHeader = "Content-Length: ";
				int contentLength = 0;
				String post = inputStream.readLine();
				boolean isPost = post.startsWith("POST");
				System.out.println("isPost::" + isPost);
				while (!(post = inputStream.readLine()).equals("")) {
					if (isPost && post.startsWith(contentHeader)) {
						contentLength = Integer.parseInt(post.substring(contentHeader.length()));
					}
				}

				StringBuilder reqBuilder = new StringBuilder();

				if (isPost) {
					int c = 0;
					for (int i = 0; i < contentLength; i++) {
						c = (char) inputStream.read();
						reqBuilder.append((char) c);
					}
				}

				JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(reqBuilder.toString());
				JSONRPC2Response jsonrpc2Response = dispatcher.process(jsonrpc2Request, null);
				
				outputStream.write("HTTP/1.1 200 OK\r\n");
				outputStream.write("Content-Type: application/json\r\n");
				outputStream.write("\r\n");
				outputStream.write(jsonrpc2Response.toJSONString());
				outputStream.flush();
				outputStream.close();

				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONRPC2ParseException e) {
			e.printStackTrace();
		}

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
