import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

public class FingerTable {

	private int guid;
	private TreeMap<Integer, InetAddress> activeNodes;
	private TreeMap<Integer, Integer> sucessors;
	private static final int I_ENTRIES = 4;
	private static final int MAX_NODES = 16;

	public FingerTable(int guid, TreeMap<Integer, InetAddress> activeNodes) {
		this.guid = guid;
		this.activeNodes = activeNodes;
		this.sucessors = new TreeMap<>();
		constructTable();
	}

	public void constructTable() {
		for (int i = 0; i < I_ENTRIES; i++) {
			int nextLiveNode = 0;
			boolean flag = false;
			Integer[] listOfNodes = (Integer[]) activeNodes.keySet().toArray();
			int findNode = ((int) (guid + Math.pow(2, i))) % MAX_NODES;

			if (activeNodes.containsKey(findNode)) {
				nextLiveNode = findNode;
			} else {
				for (int j = 0; j < listOfNodes.length; j++) {
					int id = listOfNodes[j];
					if (findNode < id) {
						flag = true;
						nextLiveNode = id;
						break;
					}
				}

				if (!flag)
					nextLiveNode = listOfNodes[0];
			}

			sucessors.put(findNode, nextLiveNode);
		}
	}
	
	public void printFingerTable() {
		System.out.println("Actual        Successor");
		for (Map.Entry<Integer, Integer> entry : sucessors.entrySet()) {
			System.out.println(entry.getKey() + "      " + entry.getValue());
		}
	}

}