
//The JSON-RPC 2.0 Base classes that define the 
//JSON-RPC 2.0 protocol messages
import com.thetransactioncompany.jsonrpc2.*;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

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

		private static HashMap<Integer, InetAddress> activeNodes;
		private Socket clientSocket;
		private PrintWriter outputStream;
		private BufferedReader inputStream;
		private Dispatcher dispatcher;

		/**
		 * Initiate all the socket related variables
		 * 
		 * @param socket
		 */
		public Handler(Socket socket) {
			System.out.println(
					"New client connected on PORT #: " + socket.getLocalPort() + " :: " + socket.getInetAddress());

			try {
				this.clientSocket = socket;
				this.outputStream = new PrintWriter(clientSocket.getOutputStream());
			} catch (IOException e) {
				System.out.println("IO Output Exception in Handler Constructor");
			}
			try {
				this.inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}

			this.dispatcher = new Dispatcher();
			dispatcher.register(new ServerHandler.SocketHandler());
		}

		/**
		 * This is where nodes connects or disconnects with peers and maintains an
		 * active nodes list
		 */
		public void run() {

			InetAddress ipAddress = clientSocket.getInetAddress();
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

				System.out.println(reqBuilder.toString());

				JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(reqBuilder.toString());
				JSONRPC2Response jsonrpc2Response = dispatcher.process(jsonrpc2Request, null);
				
				System.out.println("jsonResponse.indicatesSuccess():: " + jsonrpc2Response.indicatesSuccess());
				System.out.println("jsonResponse.indicatesSuccess():: " + jsonrpc2Response.getResult());
				outputStream.write("HTTP/1.1 200 OK\r\n");
				outputStream.write("Content-Type: application/json\r\n");
				outputStream.write("\r\n");
				outputStream.write(jsonrpc2Response.toJSONString());
				outputStream.flush();
				outputStream.close();
			} catch (IOException | JSONRPC2ParseException e) {
				e.printStackTrace();
			}

		}

	}

	public static void main(String[] args) {
		new LookupServer();
	}

}

class ServerHandler {

	public static class SocketHandler implements RequestHandler {

		@Override
		public String[] handledRequests() {
			return new String[] { "socket" };
		}

		@Override
		public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
			if (request.getMethod().equalsIgnoreCase("socket")) {
				ArrayList<Object> params = (ArrayList) request.getParams();
				return new JSONRPC2Response(params.get(0), request.getID());
			} else {
				return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
			}
		}

	}

}
