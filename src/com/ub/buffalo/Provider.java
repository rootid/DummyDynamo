package com.ub.buffalo;

public class Provider {

	private int providerKey;
	private String providerValue;
	
	public Provider() {
		
	}

	
	public Provider(int providerKey, String providerValue) {
		super();
		this.providerKey = providerKey;
		this.providerValue = providerValue;
	}


	public int getProviderKey() {
		return providerKey;
	}

	public void setProviderKey(int providerKey) {
		this.providerKey = providerKey;
	}

	public String getProviderValue() {
		return providerValue;
	}

	public void setProviderValue(String providerValue) {
		this.providerValue = providerValue;
	}
	
	
	
}
