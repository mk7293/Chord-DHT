import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

public class FingerTable {

	private int guid;
	private TreeMap<Integer, InetAddress> activeNodes;
	private TreeMap<Integer, Integer> sucessors;
	public int firstNode;
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
			Object[] listOfNodes = activeNodes.keySet().toArray();
			int findNode = ((int) (guid + Math.pow(2, i))) % MAX_NODES;
			
			if (i == 0) {
				firstNode = findNode;
			}
			
			if (activeNodes.containsKey(findNode)) {
				nextLiveNode = findNode;
			} else {
				for (int j = 0; j < listOfNodes.length; j++) {
					int id = (int) listOfNodes[j];
					if (findNode < id) {
						flag = true;
						nextLiveNode = id;
						break;
					}
				}

				if (!flag)
					nextLiveNode = (int) listOfNodes[0];
			}

			sucessors.put(findNode, nextLiveNode);
		}
	}
	
	public int getFirstNode() {
		return firstNode;
	}
	
	public void printFingerTable() {
		System.out.println("Actual\t\tSuccessor");
		for (Map.Entry<Integer, Integer> entry : sucessors.entrySet()) {
			System.out.println(entry.getKey() + "\t\t" + entry.getValue());
		}
	}
	
	public TreeMap<Integer, Integer> getFingerTable() {
		return sucessors;
	}

}
