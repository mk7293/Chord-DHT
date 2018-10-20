import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.util.NamedParamsRetriever;

/**
 * 
 * Peers contains information of its finger table and files associated with it
 * 
 * @author mk7293
 *
 */
public class Peers extends Thread {

	private int guid;
	private static int requestId;
	private String serverIPAddress;
	private int serverPort;

	private Thread thread;

	private JSONRPC2Session session;
	private JSONRPC2Request jsonRequest;
	private JSONRPC2Response jsonResponse;

	private FingerTable fingerTable;
	private boolean isNodeOnline = false;

	public Peers(int guid, String ipAddress, int port) {

		this.guid = guid;
		this.serverIPAddress = ipAddress;
		this.serverPort = port;

		thread = new Thread(this);
		thread.start();

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
					fingerTable.printFingerTable();
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
		session = new JSONRPC2Session(serverURL);
	}

	/**
	 * Join into the network
	 */
	private void joinNetwork() {
		if (!isNodeOnline) {

			connectToSocket();
			ArrayList<Object> list = new ArrayList<>();
			list.add(guid);

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
				System.out.println();
			}
		} else {
			System.out.println("Network '" + guid + "' already joined ");
		}
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
	private void constructFingerTable(TreeMap<Integer, InetAddress> activeNodes) {
		fingerTable = new FingerTable(guid, activeNodes);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private JSONRPC2Response processMethods(JSONRPC2Request request) {
		String response = "";

		switch (request.getMethod()) {
		case "UpdateFingerTable":

			Map<String, Object> tempMap = request.getNamedParams();
			TreeMap<Integer, InetAddress> activeNodes = new TreeMap<>();

			for (Map.Entry<String, Object> entry : tempMap.entrySet()) {
				
				System.out.println(entry.getValue());
				
				try {
					activeNodes.put(Integer.parseInt(entry.getKey()), InetAddress.getByName(String.valueOf(entry.getValue()).substring(1)));
				} catch (NumberFormatException | UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			constructFingerTable(activeNodes);
			response = "Finger Table Constructed";
		}

		return new JSONRPC2Response(response, request.getID());
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(4000);
			System.out.println("Server Run Started");

			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("Connected run method:: " + socket.getInetAddress());
				PrintWriter outputStream = new PrintWriter(socket.getOutputStream());
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				System.out.println("Waiting for request");

				String contentHeader = "Content-Length: ";
				int contentLength = 0;
				String post = inputStream.readLine();
				boolean isPost = post.startsWith("POST");

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

				System.out.println(reqBuilder.toString());

				JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(reqBuilder.toString());
				JSONRPC2Response jsonrpc2Response = processMethods(jsonrpc2Request);

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

		Peers peers = new Peers(Integer.parseInt(args[0]), args[1], 8000);
//		new Peers(1, "127.0.0.1", 5014);

	}

}
