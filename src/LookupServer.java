import java.net.InetAddress;
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

	// Connect with the socket
	public LookupServer(int port) {

	}

	/**
	 * 
	 * Handler Thread class are spawned from the listening loop
	 * 
	 * @author mk7293
	 *
	 */
	private static class Handler extends Thread {

		HashMap<Integer, InetAddress> activeNodes;

		/**
		 * Initiate all the socket related variables
		 * 
		 * @param socket
		 */
		public Handler(Socket socket) {

		}

		/**
		 * This is where nodes connects or disconnects with peers and maintains an
		 * active nodes list
		 */
		public void run() {

		}

	}

	public static void main(String[] args) {
		new LookupServer(5000);
	}

}
