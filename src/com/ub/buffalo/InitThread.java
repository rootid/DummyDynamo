package com.ub.buffalo;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import android.util.Log;

/**
 * 
 * @author vikram
 *
 */
public class InitThread implements Runnable{

	private static final String INIT = "Init";
	private Socket connSocket;
	private HashGenerator hashGenrator;
	private String myPort = String.valueOf(Integer.parseInt(Util.MY_PORT) * 2);
	public InitThread() {
		hashGenrator = HashGenerator.getHashInstance(Util.ALGORITHM_TYPE);
		populateKeyMap();
		populateNodeList();
		populateDummyContactList();
		//		populateContactList();
		popoulateCoOrdinatorMap(); //after Node list population
		populatePrefList();
	}


	private void populateDummyContactList() {

		Util.contactList = new ArrayList<String>();
		Util.contactList.clear();
		int index = 0;
		for(Node node:Util.nodeList) {
			if(node.getPortNumber().equalsIgnoreCase(myPort)) {
				break;			
			}
			index ++;
		}
		//predecessor 
		int lower = index;
		lower -= 1;
		if(lower < 0) {
			lower = Util.nodeList.size() - 1;
		}
		Util.contactList.add(Util.nodeList.get(lower).getPortNumber());
		
		//successor
		int upper = index;
		upper += 1;
		if(upper >= Util.nodeList.size()) {
			upper = 0;
		}
		Util.contactList.add(Util.nodeList.get(upper).getPortNumber());
					
	}
	private void populatePrefList() {
		//check wheather it is co-ordinator
		int nbrIndex = 0;
		Util.prefList.clear();		
		for(Node node:Util.nodeList) {
			//update nbrIndex
			if(node.getPortNumber().equalsIgnoreCase(myPort)) {		
				break;
			}
			nbrIndex++;		
		}

		for(int i = 0;i < Util.WRITE_QUORAM + 1;i++) {
			nbrIndex += 1;
			if(nbrIndex > Util.nodeList.size()-1) {
				nbrIndex = 0;
			}		
			Util.prefList.add(Util.nodeList.get(nbrIndex).getPortNumber());		
			//	Log.d(ACCEPT_THREAD,"pref list size :"+prefList.size()+"with port number :"+tokens[1]);
		}
	}


	private void popoulateCoOrdinatorMap() {	
		Node node1;
		Node node2;
		List<String> prefList;
		for(int j = 0;j <Util.TOTAL_KEYS;j++) {
			String input = String.valueOf(j);
			String comValue = hashGenrator.genHash(input);
			prefList = new ArrayList<String>();
			prefList.clear();
			if(comValue.compareTo(Util.nodeList.get(Util.nodeList.size()- 1).getNodeId()) >= 1) 
			{
				//send to 0th node		
				node1 = Util.nodeList.get(0);
				node2 = Util.nodeList.get(1);
				prefList.add(node1.getPortNumber());
				prefList.add(node2.getPortNumber());
				//Log.d(DHT_THREAD, "key lies after"+node1.getNodeId() +"with port"+node1.getPortNumber());
				//Log.d(DHT_THREAD, "sent to "+node1.getPortNumber() +"with node id "+node1.getNodeId());
			}
			else if(comValue.compareTo(Util.nodeList.get(0).getNodeId()) <= 1) {
				node1 = Util.nodeList.get(Util.nodeList.size()- 1);
				node2 = Util.nodeList.get(0);
				prefList.add(node1.getPortNumber());
				prefList.add(node2.getPortNumber());
			}
			else { 
				for (int i=0;i<Util.nodeList.size() -1 ;i++) {
					int index = i;
					String lowerId = Util.nodeList.get(index).getNodeId();
					String upperId = Util.nodeList.get(++index).getNodeId();
					if(comValue.compareTo(lowerId) > 0 && 
							comValue.compareTo(upperId) < 0) {
						//insert to upperId						
						node1 = Util.nodeList.get(index);
						int next = index + 1;
						prefList.add(node1.getPortNumber());
						if(next >= Util.nodeList.size()) {
							next = 0;
						}
						node2 = Util.nodeList.get(next);
						prefList.add(node2.getPortNumber());
						//Log.d(DHT_THREAD, "key lies between"+lowerId +"and "+upperId);
						//Log.d(DHT_THREAD, "sent to "+node1.getPortNumber() +"with node id "+node1.getNodeId());
					}

				}
			}

			Util.CoOrdinatorMap.put(input, prefList);
		}
	}

	private void populateKeyMap() {
		Util.keyMap = new HashMap<String, String>();
		for(int i = 0;i < Util.TOTAL_KEYS;i++) {
			String input = String.valueOf(i);
			Util.keyMap.put(hashGenrator.genHash(input),String.valueOf(i));			
		}

	}

	private void populateContactList() {
		Util.contactList = new ArrayList<String>();
		Util.contactList.clear();
		int index = 0;
		for(Node node:Util.nodeList) {
			if(node.getPortNumber().equalsIgnoreCase(myPort)) {
				break;			
			}
			index ++;
		}		
		for(int i=0;i< Util.READ_QUORAM;i++) {
			int upper = index;
			upper += 1;
			if(upper >= Util.nodeList.size()) {
				upper = 0;
			}
			Util.contactList.add(Util.nodeList.get(upper).getPortNumber());
			int lower = index;
			lower -= 1;
			if(lower < 0) {
				lower = Util.nodeList.size() - 1;
			}
			Util.contactList.add(Util.nodeList.get(lower).getPortNumber());			
		}

	}


	private void populateNodeList(){
		Util.nodeList = new ArrayList<Node>();
		Node node;
		for(int i = Util.START;i < Util.END;i = i+2){   			//5564 for 5 emulator 2 no extra
			node = new Node(hashGenrator.genHash(String.valueOf(i)), 
					String.valueOf(i*2));
			Util.nodeList.add(node);
		}
		Collections.sort(Util.nodeList,new NodeComparator());

	}


	public void run() {
		Log.d(INIT, Thread.currentThread().getName());

	}


}
