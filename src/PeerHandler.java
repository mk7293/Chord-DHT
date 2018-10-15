import java.net.InetAddress;
import java.util.List;
import java.util.TreeMap;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class PeerHandler {

	public static class SocketHandler implements RequestHandler {

		@Override
		public String[] handledRequests() {
			return new String[] { "UpdateFingerTable" };
		}

		@SuppressWarnings("unchecked")
		@Override
		public JSONRPC2Response process(JSONRPC2Request request, MessageContext context) {
			switch(request.getMethod()) {
			case "UpdateFingerTable":
				List<Object> params = (List<Object>) request.getParams();
				Peers peers = (Peers)params.get(0);
				peers.constructFingerTable((Integer) params.get(1), (TreeMap<Integer, InetAddress>) params.get(2));
				return new JSONRPC2Response("Finger Table constructed", request.getID());
			default:
				return new JSONRPC2Response("Finger Table constructed", request.getID());
			}
		}

	}
}