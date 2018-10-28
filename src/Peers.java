import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.*;

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
	private static TreeMap<Integer, InetAddress> anchorActiveNodes = new TreeMap<>();
	TreeMap<Integer, InetAddress> activeNodes;

	InetAddress routeAddr = null;
	String routeMessage = "";

	Scanner scanner = new Scanner(System.in);

	public Peers(int guid, String ipAddress, int port) {

		this.guid = guid;
		this.serverIPAddress = ipAddress;
		this.serverPort = port;

		thread = new Thread(this);
		thread.start();

		fileCollection = new ArrayList<>();
		activeNodes = new TreeMap<>();

		while (true) {
			try {

				System.out.println(
						"Chord DHT Menu \n 1. Join Network \n " + "2. Leave Network \n 3. Add File \n 4. Show Files \n "
								+ "5. Search File \n 6. Show Finger Table \n ");

				System.out.print("Please enter your option number: ");
				Scanner scanner = new Scanner(System.in);
				int option = scanner.nextInt();
				System.out.println();

				switch (option) {
				case 1:
					joinNetwork();
					break;
				case 2:
					if (isNodeOnline) {
						leaveNetwork();
					} else {
						System.out.println("Node is offline");
					}
					break;
				case 3:
					if (isNodeOnline) {
						System.out.println("Please enter the file content: ");
						String fileContent = scanner.next();

						addFile(-1, fileContent);
					} else {
						System.out.println("Node is offline");
					}
					break;
				case 4:
					if (isNodeOnline) {
						showFilesFromCurrent();
					} else {
						System.out.println("Node is offline");
					}

					break;
				case 5:
					if (isNodeOnline) {
						System.out.println("Please enter the file content: ");
						String searchFileContent = scanner.next();

						searchFile(searchFileContent);
					} else {
						System.out.println("Node is offline");
					}
					break;
				case 6:
					if (isNodeOnline) {
						fingerTable.printFingerTable();
					} else {
						System.out.println("Node is offline");
					}
					break;
				default:
					System.out.println("Please provide the correct option number");
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
			try {
				connectToSocket();
				ArrayList<Object> list = new ArrayList<>();
				list.add(guid);

				jsonRequest = new JSONRPC2Request("join", list, requestId++);
				jsonResponse = session.send(jsonRequest);

				String response = "";

				if (jsonResponse.indicatesSuccess()) {
					response = (String) jsonResponse.getResult();
					System.out.println("JSONResponse from server: " + response);
				} else {
					System.out.println("Error");
				}

				if (response.equalsIgnoreCase("Welcome Anchor GUID: " + guid)) {
					System.out.println("Anchor Node GUID: " + guid + " joined");
					anchorActiveNodes.put(guid, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
					isNodeOnline = true;

					constructFingerTable(anchorActiveNodes);
					getSuccessorFiles();

					System.out.println();
				} else {
					System.out.println("Response:: " + response);
					JSONRPC2Session rpcSession = new JSONRPC2Session(new URL("http:/" + response + ":4000/"));
					JSONRPC2Request request2 = new JSONRPC2Request("JoinPeers", requestId++);

					list.clear();
					list.add(String.valueOf(guid));
					list.add(String.valueOf(InetAddress.getLocalHost().getHostAddress()));
					request2.setPositionalParams(list);

					JSONRPC2Response response2 = rpcSession.send(request2);

					if (response2.indicatesSuccess()) {
						response = (String) response2.getResult();
						if (response.equalsIgnoreCase("Welcome GUID: " + guid)) {
							System.out.println("GUID: " + guid + " joined along with peers");
							isNodeOnline = true;
							System.out.println();
						} else {
							System.out.println(response);
							isNodeOnline = false;
							while (true) {
								System.out.println("Please provide different GUID between 0 & 15 inclusive: ");
								this.guid = scanner.nextInt();
								if (this.guid >= 0 && this.guid <= 15) {
									break;
								}
							}
						}
					} else {
						System.out.println("Error");
					}
				}
			} catch (JSONRPC2SessionException e) {
				System.err.println("JSONRPC2SessionException on JoinNetwork Method");
			} catch (UnknownHostException e) {
				System.err.println("UnknownHostException on JoinNetwork Method");
			} catch (MalformedURLException e2) {
				System.err.println("MalformedURLException on JoinNetwork Method");
			}
		} else {
			System.out.println("Network '" + guid + "' already joined ");
		}
	}

	/**
	 * Leave the Network
	 */
	private void leaveNetwork() {

		connectToSocket();
		try {
			ArrayList<Object> list = new ArrayList<>();
			list.add(String.valueOf(guid));

			jsonRequest = new JSONRPC2Request("leave", requestId++);
			jsonRequest.setPositionalParams(list);
			jsonResponse = session.send(jsonRequest);

			String response = "";

			if (jsonResponse.indicatesSuccess()) {
				response = (String) jsonResponse.getResult();
				System.out.println("JSONResponse from server: " + response);
			} else {
				System.out.println("Error");
			}

			System.out.println("succ:: " + fingerTable.getFingerTable().get(fingerTable.getFirstNode()) + ":::: "
					+ fingerTable.getFirstNode());
			transferSuccessorFiles(fingerTable.getFingerTable().get(fingerTable.getFirstNode()));

			// If else block
			System.out.println("Response:: " + response);
			JSONRPC2Session rpcSession = new JSONRPC2Session(new URL("http:/" + response + ":4000/"));
			JSONRPC2Request request2 = new JSONRPC2Request("LeavePeer", requestId++);

			list.clear();
			list.add(String.valueOf(guid));
			request2.setPositionalParams(list);

			JSONRPC2Response response2 = rpcSession.send(request2);

			isNodeOnline = false;

			if (response2.indicatesSuccess()) {
				System.out.println("Node leaves network");
			}

		} catch (JSONRPC2SessionException e) {
			System.out.println("JSONRPC2SessionException on leave network()");
		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException on leave network()");
		}
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
			System.out.println("MalformedURLException on transferFile()");
		} catch (JSONRPC2SessionException e) {
			System.out.println("JSONRPC2SessionException on transferFile()");
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
			list.add(String.valueOf(ipAddr));

			peerRequest.setPositionalParams(list);

			JSONRPC2Response peerResponse = peerSession.send(peerRequest);

			if (peerResponse.indicatesSuccess()) {
				System.out.println(peerResponse.getResult());
			} else {
				System.out.println("Error in jsonResponse from searchSuccessorPeers() method");
			}

		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException on searchSuccessorPeers()");
		} catch (JSONRPC2SessionException e) {
			System.out.println("JSONRPC2SessionException on searchSuccessorPeers()");
		}
	}

	private void searchFileInOtherPeers(int id, String searchFileContent, InetAddress ipAddr) {

		if (fingerTable.getFingerTable().containsKey(id)) {
			int successorNode = fingerTable.getFingerTable().get(id);

			if (successorNode == guid) {
				if (fileCollection.contains(searchFileContent)) {
					System.out.println("File: " + searchFileContent + " is at guid: " + guid);
				} else {
					System.out.println("No such File exists");
				}
			} else {
				searchSuccessorPeers(ipAddr, successorNode, fingerTable.getFingerTable().get(id), searchFileContent);
			}
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

			searchSuccessorPeers(ipAddr, fingerTable.getFingerTable().get(getNextSuccessorNode),
					inBetweenNodes(getNextSuccessorNode, fingerTable.getFingerTable().get(getNextSuccessorNode), id),
					searchFileContent);

		}

	}

	/**
	 * Search for the file by lookup on the finger table
	 */
	private void searchFile(String searchFileContent) {

		if (fileCollection.contains(searchFileContent)) {
			System.out.println("File: " + searchFileContent + " is at guid: " + guid);
		} else {

			int hashId = Math.abs(searchFileContent.hashCode() % 16);

			System.out.println("hashId: " + hashId);

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
	}

	private void showFilesFromCurrent() {

		if (fileCollection.isEmpty()) {
			System.err.println("No files stored in this peer guid: " + guid);
		} else {
			for (String files : fileCollection) {
				System.out.println("* " + files);
			}
		}
	}

	/**
	 * Transfer all the current node files to the successors when the node goes
	 * offline
	 */
	private void transferSuccessorFiles(int successorNode) {

		try {

			System.out.println("activeNodes.get(successorNode)" + activeNodes.get(successorNode));

			JSONRPC2Session rpcSession = new JSONRPC2Session(
					new URL("http:/" + activeNodes.get(successorNode) + ":4000"));

			JSONRPC2Request request = new JSONRPC2Request("TransferAllFiles", requestId++);
			ArrayList<Object> list = new ArrayList<>();

			for (String string : fileCollection) {
				list.add(string);
			}

			fileCollection.clear();

			request.setPositionalParams(list);

			JSONRPC2Response response = rpcSession.send(request);

			if (response.indicatesSuccess()) {
				System.out.println(response.getResult());
			} else {
				System.out.println("Error");
			}

		} catch (MalformedURLException e) {
			System.out.println("MalformedURLException on transferSuccessorFiles()");
		} catch (JSONRPC2SessionException e) {
			System.out.println("JSONRPC2SessionException on transferSuccessorFiles()");
		}

	}

	/**
	 * Get the file from the successor when the nodes comes online
	 */
	private void getSuccessorFiles() {
		System.out.println("Inside getSuccessorFiles()");
		if (isNodeOnline) {
			int successorNode = fingerTable.getFingerTable().get(fingerTable.getFirstNode());
			if (guid != successorNode) {
				System.out.println(activeNodes.get(successorNode));
				try {

					JSONRPC2Session session = new JSONRPC2Session(
							new URL("http:/" + activeNodes.get(successorNode) + ":4000"));

					JSONRPC2Request request = new JSONRPC2Request("GetAllFiles", requestId++);

					ArrayList<Object> list = new ArrayList<>();
					list.add(String.valueOf(guid));
					request.setPositionalParams(list);

					JSONRPC2Response response = session.send(request);

					if (response.indicatesSuccess()) {

						String files = (String) response.getResult();

						if (files.contains("%%%%%")) {
							String[] allFiles = files.split("%%%%%");

							for (int i = 0; i < allFiles.length; i++) {
								fileCollection.add(allFiles[i]);
							}
						}

					} else {
						System.out.println("Json Response Error in getSuccessorFiles()");
					}

				} catch (MalformedURLException e) {
					System.out.println("MalformedURLException on getSuccessorFiles()");
				} catch (JSONRPC2SessionException e) {
					System.out.println("JSONRPC2SessionException on getSuccessorFiles()");
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

	public void sendActiveNodes() {
		Map<String, Object> temps = new TreeMap<String, Object>();
		for (Map.Entry<Integer, InetAddress> entry : anchorActiveNodes.entrySet()) {
			activeNodes.put(entry.getKey(), entry.getValue());
			temps.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}

		for (Map.Entry<Integer, InetAddress> entry : anchorActiveNodes.entrySet()) {

			if (entry.getKey() == guid) {
				constructFingerTable(anchorActiveNodes);
				continue;
			}

			try {
				JSONRPC2Session rpcSession = new JSONRPC2Session(new URL("http:/" + entry.getValue() + ":4000/"));

				JSONRPC2Request request = new JSONRPC2Request("UpdateFingerTable", requestId++);
				request.setNamedParams(temps);
				JSONRPC2Response jsonrpc2Response = rpcSession.send(request);

				if (jsonrpc2Response.indicatesSuccess()) {
					System.out.println(
							"ActiveNodes send to client: " + entry.getValue() + " and " + jsonrpc2Response.getResult());
				} else {
					System.out.println("Error");
				}

			} catch (IOException e) {
				System.out.println("IOException on sendActiveNodes()");
			} catch (JSONRPC2SessionException e) {
				System.out.println("JSONRPC2SessionException on sendActiveNodes()");
			}
		}
	}

	private JSONRPC2Response processMethods(JSONRPC2Request request) {
		String response = "";

		ArrayList<Object> tempList;

		switch (request.getMethod()) {
		case "JoinPeers":
			tempList = (ArrayList<Object>) request.getPositionalParams();

			int peerGuid = Integer.parseInt((String) tempList.get(0));
			String ip = (String) tempList.get(1);
			try {
				InetAddress address = InetAddress.getByName(ip);
				if (!anchorActiveNodes.containsKey(peerGuid)) {
					synchronized (anchorActiveNodes) {
						anchorActiveNodes.put(peerGuid, address);
					}
					response = "Welcome GUID: " + peerGuid;
				} else {
					response = "GUID: " + guid + " already in use";
				}
			} catch (UnknownHostException e1) {
				System.out.println("UnknownHostException on processMethods()");
			}
			break;
		case "LeavePeer":
			tempList = (ArrayList<Object>) request.getPositionalParams();
			peerGuid = Integer.parseInt((String) tempList.get(0));

			synchronized (anchorActiveNodes) {
				anchorActiveNodes.remove(peerGuid);
			}

			break;
		case "UpdateFingerTable":

			Map<String, Object> tempMap = request.getNamedParams();
			activeNodes.clear();
			for (Map.Entry<String, Object> entry : tempMap.entrySet()) {

				try {
					activeNodes.put(Integer.parseInt(entry.getKey()),
							InetAddress.getByName(String.valueOf(entry.getValue()).substring(1)));
				} catch (NumberFormatException | UnknownHostException e) {
					System.out.println("UnknownHostException on processMethods()");
				}
			}

			constructFingerTable(activeNodes);
			response = "Finger Table Constructed";

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
		case "TransferAllFiles":
			tempList = (ArrayList<Object>) request.getPositionalParams();
			for (Object object : tempList) {
				fileCollection.add((String) object);
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
				ip = (String) tempList.get(2);
				routeAddr = InetAddress.getByName(ip.substring(1));
			} catch (UnknownHostException e) {
				System.out.println("UnknownHostException on processMethods()");
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

	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(4000);

			while (true) {
				Socket socket = serverSocket.accept();
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

				String peerMethod = jsonrpc2Request.getMethod();

				outputStream.write("HTTP/1.1 200 OK\r\n");
				outputStream.write("Content-Type: application/json\r\n");
				outputStream.write("\r\n");
				outputStream.write(jsonrpc2Response.toJSONString());
				outputStream.flush();
				outputStream.close();

				socket.close();

				if (peerMethod.equalsIgnoreCase("JoinPeers") || peerMethod.equalsIgnoreCase("LeavePeer")) {
					System.out.println(" \n Sending Live Nodes");
					sendActiveNodes();
				}

				if (peerMethod.equalsIgnoreCase("UpdateFingerTable")) {
					System.out.println(" \n Getting Nodes from Successor");
					getSuccessorFiles();
				}

				if (routeMessage.length() != 0) {
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

				}
			}
		} catch (IOException e) {
			System.out.println("IOException on run()");
		} catch (JSONRPC2ParseException e) {
			System.out.println("JSONRPC2ParseException on run()");
		} catch (JSONRPC2SessionException e) {
			System.out.println("JSONRPC2SessionException on run()");
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
			System.exit(0);
		}

		if (Integer.parseInt(args[0]) < 0 && Integer.parseInt(args[0]) > 15) {
			System.err.println("Please provide different GUID between 0 & 15 inclusive");
			System.exit(0);
		}

		new Peers(Integer.parseInt(args[0]), args[1], 8000);
	}

}
