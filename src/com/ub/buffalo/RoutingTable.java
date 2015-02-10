package com.ub.buffalo;

/**
 * 
 * @author vikram
 *
 */
public class RoutingTable {

	public enum NodeType{
		SUCCESSOR,
		OTHER,
		SELF,
		CO_ORDINATOR
	}
	
	private NodeType nodeType;
	private Node node;
	
	public RoutingTable() {
		
	}
		
	public RoutingTable(NodeType nodeType, Node node) {
		super();
		this.nodeType = nodeType;
		this.node = node;
	}


	public NodeType getNodeType() {
		return nodeType;
	}
	public void setNodeType(NodeType nodeType) {
		this.nodeType = nodeType;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	
	
}
