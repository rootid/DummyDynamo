package com.ub.buffalo;

import java.io.Serializable;

/**
 * 
 * @author vikram
 *
 */
public class KeyId implements Comparable<KeyId>,Serializable{

	private String key;
	private GeneralMsgFormat msgFormat;
		
	public KeyId() {
	
	}
		
	public KeyId(String key, GeneralMsgFormat msgFormat) {
		super();
		this.key = key;
		this.msgFormat = msgFormat;
	}

	@Override
	public String toString() {
		return this.msgFormat.getMsg() + ":::" + this.getKey();
	}

	public int compareTo(KeyId next) {
		
		String nextKey = ((KeyId) next).getKey(); 
		 
		//descending order
//		return nextKey.compareTo(key);
		//ascending order
		return key.compareTo(nextKey);
	}


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public GeneralMsgFormat getMsgFormat() {
		return msgFormat;
	}


	public void setMsgFormat(GeneralMsgFormat msgFormat) {
		this.msgFormat = msgFormat;
	}

	
	
}
