
//The JSON-RPC 2.0 Base classes that define the 
//JSON-RPC 2.0 protocol messages
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
//The JSON-RPC 2.0 server framework package
import com.thetransactioncompany.jsonrpc2.server.*;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minidev.json.JSONObject;

/**
 * 
 * Server Network to keep track of all the active nodes and sends the list of
 * those active nodes to all the active peers when the nodes comes online or
 * goes offline
 * 
 * @author mk7293
 *
 */
public class LookupServer {
	private final int PORT = 8000;

	// Connect with the socket
	public LookupServer() {
		try {
			ServerSocket serverSocket = new ServerSocket(PORT);
			System.out.println("Server Socket is listening");
			while (true) {
				new Handler(serverSocket.accept()).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Handler Thread class are spawned from the listening loop
	 * 
	 * @author mk7293
	 *
	 */
	private static class Handler extends Thread {

		private Socket clientSocket;
		private PrintWriter outputStream;
		private BufferedReader inputStream;
		private static TreeMap<Integer, InetAddress> activeNodes = new TreeMap<>();
		private static HashMap<Integer, InetAddress> anchorNodes = new HashMap<>();
		private static int requestId = 0;

		/**
		 * Initiate all the socket related variables
		 * 
		 * @param socket
		 */
		public Handler(Socket socket) {
			System.out
					.println("Client connected on PORT #: " + socket.getLocalPort() + " :: " + socket.getInetAddress());

			try {
				this.clientSocket = socket;
				this.outputStream = new PrintWriter(clientSocket.getOutputStream());
				this.inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				System.out.println("IO Output Exception in Lookup Peer Handler Constructor");
			}
		}

		/**
		 * This is where nodes connects or disconnects with peers and maintains an
		 * active nodes list
		 */
		public void run() {

			if (anchorNodes.isEmpty()) {
			
			}
			
			
			
			
			
			
			
			String contentHeader = "Content-Length: ";
			int contentLength = 0;

			try {
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
				JSONRPC2Response jsonrpc2Response = processMethods(jsonrpc2Request, clientSocket.getInetAddress());

				outputStream.write("HTTP/1.1 200 OK\r\n");
				outputStream.write("Content-Type: application/json\r\n");
				outputStream.write("\r\n");
				outputStream.write(jsonrpc2Response.toJSONString());

				outputStream.flush();
				outputStream.close();
				clientSocket.close();

				sendActiveNodes();

			} catch (IOException | JSONRPC2ParseException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings({ "unchecked", "deprecation" })
		public JSONRPC2Response processMethods(JSONRPC2Request request, InetAddress ipAddr) {

			String response = "";
			switch (request.getMethod()) {
			case "join":
				ArrayList<Object> params = (ArrayList<Object>) request.getParams();

				long lo = (long) params.get(0);
				int guid = (int) lo;

				if (!activeNodes.containsKey(guid)) {
					activeNodes.put(guid, ipAddr);
					response = "Welcome GUID: " + guid;
				} else {
					response = "GUID: " + guid + " already in use";
				}

			case "leave":
				break;
			}

			return new JSONRPC2Response(response, request.getID());
		}

		public void sendActiveNodes() {
			Map<String, Object> temps = new TreeMap<String, Object>();
			for (Map.Entry<Integer, InetAddress> entry : activeNodes.entrySet()) {
				temps.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
			}
			
			for (Map.Entry<Integer, InetAddress> entry : activeNodes.entrySet()) {
				System.out.println("ipaddr:: " + entry.getValue());
				try {
					JSONRPC2Session session = new JSONRPC2Session(new URL("http:/" + entry.getValue() + ":4000/"));
					System.out.println("size:: " + temps.size() + "::::" + temps.get("1"));
					
					JSONRPC2Request request = new JSONRPC2Request("UpdateFingerTable", requestId++);
					request.setNamedParams(temps);
					JSONRPC2Response jsonrpc2Response = session.send(request);

					if (jsonrpc2Response.indicatesSuccess()) {
						System.out.println("ActiveNodes send to client: " + entry.getValue() + " and "
								+ jsonrpc2Response.getResult());
					} else {
						System.out.println("Error");
					}

				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONRPC2SessionException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		new LookupServer();
	}

}