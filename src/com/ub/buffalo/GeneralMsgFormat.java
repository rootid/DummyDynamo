package com.ub.buffalo;

import java.io.Serializable;

public class GeneralMsgFormat implements Serializable {

	public enum	MsgType{
		CONNECT,
		REPLICATE,
		INSERT,
		QUERY,
		RECOVER,
		RETRY,
		RETRY_QUERY,
		REPLICATE_QUERY,
		INSERT_CO_ORDINATOR
	}
	
	private MsgType msgType;
	private String msg;
	
	public GeneralMsgFormat() {
		
	}
	
	public GeneralMsgFormat(MsgType msgType, String msg) {
		super();
		this.msgType = msgType;
		this.msg = msg;
	}

	public MsgType getMsgType() {
		return msgType;
	}
	
	public void setMsgType(MsgType msgType) {
		this.msgType = msgType;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}	
}
