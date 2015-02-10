package com.ub.buffalo;

import java.util.Comparator;

public class NodeComparator implements Comparator<Node> {

	public int compare(Node lhs, Node rhs) {		
		return lhs.getNodeId().compareTo(rhs.getNodeId());
	}

}
