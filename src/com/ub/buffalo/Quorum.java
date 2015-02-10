package com.ub.buffalo;

import java.io.Serializable;

/**
 * 
 * @author vikram
 *
 */
public class Quorum implements Serializable{

	private KeyId keyId;
	private int vote;
	
	
	public Quorum() {
	
	}
	
	public Quorum(KeyId keyId, int vote) {
		super();
		this.keyId = keyId;
		this.vote = vote;
	}


	@Override
	public String toString() {	
		return this.getKeyId().getMsgFormat().getMsg();
	}
	public KeyId getKeyId() {
		return keyId;
	}

	public void setKeyId(KeyId keyId) {
		this.keyId = keyId;
	}

	public int getVote() {
		return vote;
	}

	public void setVote(int vote) {
		this.vote = vote;
	}
		
}
