
//The JSON-RPC 2.0 Base classes that define the 
//JSON-RPC 2.0 protocol messages
import com.thetransactioncompany.jsonrpc2.*;
//The JSON-RPC 2.0 server framework package

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

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
	@SuppressWarnings("resource")
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
		private static HashMap<Integer, InetAddress> anchorNodes = new HashMap<>();
		private static InetAddress anchorIpAddress;

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

			String contentHeader = "Content-Length: ";
			int contentLength = 0;

			try {
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
				clientSocket.close();

			} catch (IOException | JSONRPC2ParseException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings({ "unchecked", "deprecation" })
		public JSONRPC2Response processMethods(JSONRPC2Request request) {

			String response = "";
			ArrayList<Object> params;
			
			switch (request.getMethod()) {
			case "join":
				params = (ArrayList<Object>) request.getParams();

				long lo = (long) params.get(0);
				int guid = (int) lo;

				if (anchorNodes.isEmpty()) {
					synchronized (anchorNodes) {
						anchorIpAddress = clientSocket.getInetAddress();
						anchorNodes.put(guid, anchorIpAddress);
						response = "Welcome Anchor GUID: " + guid;
					}
				} else {
					response = String.valueOf(anchorIpAddress);
				}
				
				break;

			case "leave":
				
				params = (ArrayList<Object>) request.getPositionalParams();
				guid = Integer.parseInt((String) params.get(0));
				
				if (anchorNodes.containsKey(guid)) {
					
				} else {
					response = String.valueOf(anchorIpAddress);
				}
				
				break;
			}

			return new JSONRPC2Response(response, request.getID());
		}
	}

	public static void main(String[] args) {
		new LookupServer();
	}

}