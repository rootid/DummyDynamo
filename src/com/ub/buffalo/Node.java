package com.ub.buffalo;

/**
 * 
 * @author vikram
 *
 */
public class Node {
	private String nodeId;
	private String portNumber;
	
	public Node() {
		
	}
		
	public Node(String nodeId, String portNumber) {
		super();
		this.nodeId = nodeId;
		this.portNumber = portNumber;
	}


	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public String getPortNumber() {
		return portNumber;
	}
	public void setPortNumber(String portNumber) {
		this.portNumber = portNumber;
	}

	
	
}
