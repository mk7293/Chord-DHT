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

	ArrayList<String> fileCollection;
	TreeMap<Integer, InetAddress> activeNodes;

	InetAddress routeAddr = null;
	String routeMessage = "";

	public Peers(int guid, String ipAddress, int port) {

		this.guid = guid;
		this.serverIPAddress = ipAddress;
		this.serverPort = port;

		thread = new Thread(this);
		thread.start();

		fileCollection = new ArrayList<>();
		activeNodes = new TreeMap<>();

		while (true) {
			Scanner scanner = new Scanner(System.in);

			try {

				System.out.println(
						"Chord DHT Menu \n 1. Join Network \n " + "2. Leave Network \n 3. Add File \n 4. Show Files \n "
								+ "5. Search File \n 6. Show Finger Table \n ");

				int option = scanner.nextInt();

				switch (option) {
				case 1:
					joinNetwork();
					if (!isNodeOnline) {
						System.out.println("Please provide different GUID between 0 & 15 inclusive: ");
						this.guid = scanner.nextInt();
					}
					break;
				case 2:

					break;
				case 3:
					System.out.println("Please enter the file content: ");
					String fileContent = scanner.next();

					addFile(-1, fileContent);

					break;
				case 4:
					showFilesFromCurrent();

					break;
				case 5:
					System.out.println("Please enter the file content: ");
					String searchFileContent = scanner.next();

					searchFile(searchFileContent);
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
				
			} else {
				System.out.println("GUID already exists");
				isNodeOnline = false;
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

	private int findDistance(int keyId, int destNodeId) {
		int distance = 1;
		while (true) {
			if ((distance++ + keyId) % 16 == destNodeId)
				return distance - 1;
		}
	}

	private int inBetweenNodes(int actualNode, int successorNode, int destNodeId) {
		if (actualNode > successorNode) {
			successorNode += 16;
			if (actualNode > destNodeId && destNodeId < successorNode)
				destNodeId += 16;
		}

		if (actualNode < destNodeId && destNodeId < successorNode) {
			return successorNode % 16;
		} else {
			return destNodeId % 16;
		}
	}

	private void transferFile(int toSend, int successorNode, String fileContent) {

		System.out.println("File: " + fileContent + " routed to " + successorNode);

		try {
			JSONRPC2Session peerSession = new JSONRPC2Session(
					new URL("http:/" + activeNodes.get(successorNode) + ":4000"));

			JSONRPC2Request peerRequest = new JSONRPC2Request("InsertTransferedFile", requestId++);

			ArrayList<Object> list = new ArrayList<>();
			list.add(String.valueOf(toSend));
			list.add(fileContent);

			peerRequest.setPositionalParams(list);

			JSONRPC2Response peerResponse = peerSession.send(peerRequest);

			if (peerResponse.indicatesSuccess()) {
				System.out.println(peerResponse.getResult());
			} else {
				System.out.println("Error in jsonResponse from transferFile() method");
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JSONRPC2SessionException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Add the files into the network when the hashed file code matches with its
	 * peer id
	 */
	private void addFile(int id, String fileContent) {
		int destNodeId = 0;
		if (id == -1)
			destNodeId = fileContent.hashCode() % 16;
		else
			destNodeId = id;

		System.out.println("HashId: " + destNodeId);

		if (destNodeId == guid) {
			System.out.println("File: " + fileContent + " inserted at GUID: " + guid);
			fileCollection.add(fileContent);
		} else if (fingerTable.getFingerTable().containsKey(destNodeId)) {
			int successorNode = fingerTable.getFingerTable().get(destNodeId);

			if (successorNode == guid) {
				System.out.println("File: " + fileContent + " inserted at GUID: " + guid);
				fileCollection.add(fileContent);
			} else {
				System.out.println("File: " + fileContent + " routed to " + successorNode);
				transferFile(successorNode, successorNode, fileContent);
			}
		} else {
			System.out.println("Not to successor");
			int min = Integer.MAX_VALUE;
			int getNextSuccessorNode = -1;
			for (Map.Entry<Integer, Integer> entry : fingerTable.getFingerTable().entrySet()) {
				int keyId = entry.getKey();
				int distance = findDistance(keyId, destNodeId);

				if (min > distance) {
					min = distance;
					getNextSuccessorNode = keyId;
				}
			}

			int successorNode = fingerTable.getFingerTable().get(getNextSuccessorNode);

			System.out.println("ggetNext: " + getNextSuccessorNode + ":::: success:: " + successorNode);
			if (successorNode == guid) {
				System.out.println("File: " + fileContent + " inserted at GUID: " + guid);
				fileCollection.add(fileContent);
			} else {

				transferFile(inBetweenNodes(getNextSuccessorNode, successorNode, destNodeId), successorNode,
						fileContent);
			}
		}
	}

	private void searchSuccessorPeers(InetAddress ipAddr, int successorNode, int id, String searchFileContent) {

		System.out.println("Contacting successor: " + successorNode + " from the node: " + guid
				+ " to search for file: " + searchFileContent);

		try {
			JSONRPC2Session peerSession = new JSONRPC2Session(
					new URL("http:/" + activeNodes.get(successorNode) + ":4000"));

			JSONRPC2Request peerRequest = new JSONRPC2Request("SearchFile", requestId++);

			ArrayList<Object> list = new ArrayList<>();

			list.add(String.valueOf(id));
			list.add(searchFileContent);
			list.add(ipAddr);

			peerRequest.setPositionalParams(list);

			JSONRPC2Response peerResponse = peerSession.send(peerRequest);

			if (peerResponse.indicatesSuccess()) {
				System.out.println(peerResponse.getResult());
			} else {
				System.out.println("Error in jsonResponse from searchSuccessorPeers() method");
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (JSONRPC2SessionException e) {
			e.printStackTrace();
		}
	}

	private void searchFileInOtherPeers(int id, String searchFileContent, InetAddress ipAddr) {

		if (fingerTable.getFingerTable().containsKey(id)) {
			searchSuccessorPeers(ipAddr, fingerTable.getFingerTable().get(id), fingerTable.getFingerTable().get(id),
					searchFileContent);
		} else {
			int min = Integer.MAX_VALUE;
			int getNextSuccessorNode = -1;

			for (Map.Entry<Integer, Integer> entry : fingerTable.getFingerTable().entrySet()) {
				int keyId = entry.getKey();
				int distance = findDistance(keyId, id);

				if (min > distance) {
					min = distance;
					getNextSuccessorNode = keyId;
				}
			}

			searchSuccessorPeers(ipAddr, fingerTable.getFingerTable().get(id),
					inBetweenNodes(getNextSuccessorNode, fingerTable.getFingerTable().get(getNextSuccessorNode), id),
					searchFileContent);

		}

	}

	/**
	 * Search for the file by lookup on the finger table
	 */
	private void searchFile(String searchFileContent) {

		int hashId = Math.abs(searchFileContent.hashCode() % 16);

		if (hashId == guid) {
			if (fileCollection.contains(searchFileContent)) {
				System.out.println("File: " + searchFileContent + " is at guid: " + guid);
			} else {
				System.out.println("No such File exists");
			}
		} else {
			searchFileInOtherPeers(hashId, searchFileContent, activeNodes.get(guid));
		}
	}

	private void showFilesFromCurrent() {

		if (fileCollection.isEmpty()) {
			System.err.println("No files stored in this peer guid: " + guid);
		} else {
			for (String files : fileCollection) {
				System.out.println("*" + files);
			}
		}
	}

	/**
	 * Get the file from the successor when the nodes comes online
	 */
	private void getSuccessorFiles() {
		if (isNodeOnline) {
			int successorNode = fingerTable.getFingerTable().get(fingerTable.getFirstNode());
			if (guid != successorNode) {
				try {
					JSONRPC2Session session = new JSONRPC2Session(
							new URL("http:/" + activeNodes.get(successorNode) + ":4000"));

					JSONRPC2Request request = new JSONRPC2Request("GetAllFiles", requestId++);

					ArrayList<Object> list = new ArrayList<>();
					list.add(String.valueOf(guid));
					request.setPositionalParams(list);

					JSONRPC2Response response = session.send(request);
					
					System.out.println("resdsdsd:::" + response.getResult());
					
					if (response.indicatesSuccess()) {
						
						System.out.println("res:::" + response.getResult());
						
						String files = (String) response.getResult();
						String[] allFiles = files.split("%%%%%");

						for (int i = 0; i < allFiles.length; i++) {
							fileCollection.add(allFiles[i]);
						}

					} else {
						System.out.println("Json Response Error in getSuccessorFiles()");
					}

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				}
			}
		}
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

		ArrayList<Object> tempList;

		switch (request.getMethod()) {
		case "UpdateFingerTable":

			Map<String, Object> tempMap = request.getNamedParams();

			for (Map.Entry<String, Object> entry : tempMap.entrySet()) {

				System.out.println(entry.getValue());

				try {
					activeNodes.put(Integer.parseInt(entry.getKey()),
							InetAddress.getByName(String.valueOf(entry.getValue()).substring(1)));
				} catch (NumberFormatException | UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			constructFingerTable(activeNodes);
			response = "Finger Table Constructed";
			getSuccessorFiles();
			break;
		case "GetAllFiles":
			if (isNodeOnline && !fileCollection.isEmpty()) {
				System.out.println("Sending the files to the Actual Node");

				tempList = (ArrayList<Object>) request.getPositionalParams();
				int actualGuid = Integer.parseInt((String) tempList.get(0));

				int i = 0;
				while (i < fileCollection.size()) {
					String file = fileCollection.get(i);
					int fileHashId = Math.abs(file.hashCode() % 16);

					if (actualGuid == inBetweenNodes(guid, actualGuid, fileHashId)) {
						response += file + "%%%%%";
						fileCollection.remove(i);
						continue;
					}

					i++;
				}
			}

			break;
		case "InsertTransferedFile":
			tempList = (ArrayList<Object>) request.getPositionalParams();

			int idToInsert = Integer.parseInt((String) tempList.get(0));
			String fileContent = (String) tempList.get(1);

			addFile(idToInsert, fileContent);
			break;
		case "SearchFile":
			tempList = (ArrayList<Object>) request.getPositionalParams();

			int fileId = Integer.parseInt((String) tempList.get(0));
			String searchFileContent = (String) tempList.get(1);
			try {
				routeAddr = InetAddress.getByName((String) tempList.get(1));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

			if (fileId == guid) {
				if (fileCollection.contains(searchFileContent)) {
					routeMessage = "File found at " + guid;
				} else {
					routeMessage = "No such file exists";
				}
			} else {
				searchFileInOtherPeers(fileId, searchFileContent, routeAddr);
			}
			break;
		case "PrintResponse":
			tempList = (ArrayList<Object>) request.getPositionalParams();

			System.out.println((String) tempList.get(0));
		}

		return new JSONRPC2Response(response, request.getID());
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(4000);

			while (true) {
				Socket socket = serverSocket.accept();
//				System.out.println("Connected run method:: " + socket.getInetAddress());
				PrintWriter outputStream = new PrintWriter(socket.getOutputStream());
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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

				JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(reqBuilder.toString());
				JSONRPC2Response jsonrpc2Response = processMethods(jsonrpc2Request);

				outputStream.write("HTTP/1.1 200 OK\r\n");
				outputStream.write("Content-Type: application/json\r\n");
				outputStream.write("\r\n");
				outputStream.write(jsonrpc2Response.toJSONString());
				outputStream.flush();
				outputStream.close();

				socket.close();

				if (routeMessage.length() != 0) {
					try {
						JSONRPC2Session peerSession = new JSONRPC2Session(new URL("http:/" + routeAddr + ":4000"));

						JSONRPC2Request peerRequest = new JSONRPC2Request("PrintResponse", requestId++);

						ArrayList<Object> list = new ArrayList<>();

						list.add(routeMessage);
						peerRequest.setPositionalParams(list);

						JSONRPC2Response peerResponse = peerSession.send(peerRequest);

						if (peerResponse.indicatesSuccess()) {
							System.out.println(peerResponse.getResult());
						} else {
							System.out.println("Error in jsonResponse from searchSuccessorPeers() method");
						}

						routeMessage = "";

					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (JSONRPC2SessionException e) {
						e.printStackTrace();
					}
				}
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
