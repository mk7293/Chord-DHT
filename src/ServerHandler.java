import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

class ServerHandler {

	public static class SocketHandler implements RequestHandler {

		private static TreeMap<Integer, InetAddress> activeNodes;
		private static int requestId = 0;

		@Override
		public String[] handledRequests() {
			return new String[] { "join", "getLiveNodes", "leave" };
		}

		@SuppressWarnings({ "unchecked", "deprecation" })
		@Override
		public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
			if (request.getMethod().equalsIgnoreCase("join")) {
				ArrayList<Object> params = (ArrayList) request.getParams();

				if (!activeNodes.containsKey((Integer) params.get(0))) {
					activeNodes.put((Integer) params.get(0), (InetAddress) params.get(1));
					return new JSONRPC2Response("Welcome GUID: " + (Integer) params.get(0), request.getID());
				} else {
					return new JSONRPC2Response("GUID: " + (Integer) params.get(0) + " already in use",
							request.getID());
				}
			} else if (request.getMethod().equalsIgnoreCase("getliveNodes")) {

				ArrayList<Object> list = (ArrayList<Object>) request.getParams();
				list.add(activeNodes);

				for (Map.Entry<Integer, InetAddress> entry : activeNodes.entrySet()) {
					InetAddress ip = entry.getValue();
					System.out.println("Sending List of Active Nodes to " + ip);
					try {
						JSONRPC2Session session = new JSONRPC2Session(new URL("http://" + ip + ":" + 7000 + "/"));

						JSONRPC2Request jsonRPC2Request = new JSONRPC2Request("UpdateFingerTable", list, requestId++);
						JSONRPC2Response jsonrpc2Response = session.send(jsonRPC2Request);

					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (JSONRPC2SessionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, request.getID());
			}
		}

	}

}